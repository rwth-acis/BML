package i5.bml.parser.walker;

import i5.bml.parser.errors.ParserException;
import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import i5.bml.parser.types.*;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

import static i5.bml.parser.errors.ParserError.*;

public class TypeSynthesizer extends BMLBaseListener {

    private final Scope currentScope;

    public TypeSynthesizer(Scope currentScope) {
        this.currentScope = currentScope;
    }

    @Override
    public void exitComponent(BMLParser.ComponentContext ctx) {
        AbstractBMLType resolvedType = (AbstractBMLType) TypeRegistry.resolveType(ctx.typeString);

        // Instantiate fields of `resolvedType` with component parameters
        for (var param : ctx.params.elementValuePair()) {
            // Check: Component instantiation parameter is defined by component type class
            var field = Arrays.stream(resolvedType.getClass().getDeclaredFields())
                    .filter(f -> param.name.getText().equals(f.getName()))
                    .findAny();

            if (field.isEmpty()) {
                throw new ParserException(NOT_DEFINED_FOR.format(param.name.getText(), resolvedType.getName()), param.name);
            }

            var canAccess = field.get().canAccess(resolvedType);
            field.get().setAccessible(true);

            var terminalNodeType = ((TerminalNode) param.value.getChild(0)).getSymbol().getType();
            var literalInstance = switch (terminalNodeType) {
                case BMLParser.IntegerLiteral, BMLParser.FloatingPointLiteral -> new BigDecimal(param.value.getText());
                case BMLParser.StringLiteral -> param.value.getText().substring(1, param.value.getText().length() - 1);
                case BMLParser.BooleanLiteral -> Boolean.parseBoolean(param.value.getText());
                default -> throw new ParserException(UNKNOWN_TYPE.format(terminalNodeType),
                        ((TerminalNode) param.value.getChild(0)).getSymbol());
            };

            try {
                field.get().set(resolvedType, literalInstance);
            } catch (IllegalAccessException e) {
                // TODO
                throw new RuntimeException(e);
            }

            field.get().setAccessible(canAccess);
        }

        // Invoke registered initializer methods of `resolvedType`
        Arrays.stream(resolvedType.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(BMLInitializerMethod.class))
                .forEach(m -> {
                    try {
                        m.invoke(resolvedType);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new ParserException(ExceptionUtils.getRootCause(e).getMessage(), ctx);
                    }
                });

        // Lastly, set `resolvedType` for symbol (we assume that it was created by the ST i5.bml.parser.walker)
        var v = currentScope.resolve(ctx.name.getText());
        ((VariableSymbol) v).setType(resolvedType);
    }

    @Override
    public void exitAssignment(BMLParser.AssignmentContext ctx) {
        // When exiting an assignment, we can assume that the right-hand side type was computed already,
        // since it is an expression
        if (ctx.op.getText().equals("=")) {
            VariableSymbol v = new VariableSymbol(ctx.name.getText());
            v.setType(ctx.expression().type);
            currentScope.define(v);
        } else { // Assignment operators with simultaneous operation
            // Make sure left-hand side is declared
            var v = currentScope.resolve(ctx.name.getText());
            if (!(v instanceof VariableSymbol)) {
                throw new ParserException(NOT_DEFINED.format(ctx.name.getText()), ctx.name);
            }

            // Type of left-hand side should already be set
            var leftType = ((VariableSymbol) v).getType();
            var rightType = ctx.expression().type;
            if (!(leftType instanceof BMLNumber)) {
                throw new ParserException(EXPECTED_BUT_FOUND.format("Number", leftType), ctx.name);
            } else if (!(rightType instanceof BMLNumber)) {
                throw new ParserException(EXPECTED_BUT_FOUND.format("Number", rightType), ctx.name);
            }
        }
    }

    @Override
    public void exitLiteral(BMLParser.LiteralContext ctx) {
        var terminalNodeType = ((TerminalNode) ctx.getChild(0)).getSymbol().getType();
        ctx.type = switch (terminalNodeType) {
            case BMLParser.IntegerLiteral -> new BMLNumber(false);
            case BMLParser.FloatingPointLiteral -> new BMLNumber(true);
            case BMLParser.StringLiteral -> new BMLString();
            case BMLParser.BooleanLiteral -> new BMLBoolean();
            default -> {
                throw new ParserException("Unknown type %s".formatted(terminalNodeType), (Token) ctx.getChild(0));
            }
        };
    }

    @Override
    public void exitAtom(BMLParser.AtomContext ctx) {
        if (ctx.literal() != null) {
            ctx.type = ctx.literal().type;
        } else { // Identifier
            var name = ctx.Identifier().getText();
            var r = currentScope.resolve(name);
            if (!(r instanceof VariableSymbol)) {
                throw new ParserException(NOT_DEFINED.format(name), ctx.Identifier().getSymbol());
            }

            ctx.type = ((VariableSymbol) r).getType();
        }
    }

    @Override
    public void exitExpression(BMLParser.ExpressionContext ctx) {
        if (ctx.atom() != null) {
            ctx.type = ctx.atom().type;
        } else if (ctx.op != null) {
            switch (ctx.op.getText()) {
                case "(" -> {
                    try {
                        ctx.type = ctx.expr.type.getClass().getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "." -> {
                    Type prevType = ctx.expr.type;
                    var currentCtx = ctx.Identifier() != null ? ctx.Identifier() : ctx.functionCall();

                    // Check: type allows '.field/.method()/.list[]' -> delegate check to class of prevType
                    var accessResolver = Arrays.stream(prevType.getClass().getDeclaredMethods())
                            .filter(m -> m.getName().equals("resolveAccess"))
                            .findAny();

                    Type resolvedType;
                    try {
                        //noinspection OptionalGetWithoutIsPresent
                        resolvedType = (Type) accessResolver.get().invoke(prevType, currentCtx);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchElementException e) {
                        throw new IllegalStateException("No resolveAccess method for class %s".formatted(prevType.getClass()));
                    }

                    if (resolvedType == null) {
                        if (ctx.Identifier() != null) {
                            throw new ParserException("Could not resolve `%s` for %s".formatted(currentCtx.getText(), prevType),
                                    ctx.Identifier().getSymbol());
                        } else {
                            throw new ParserException("Could not resolve `%s` for %s".formatted(currentCtx.getText(), prevType),
                                    ctx.functionCall());
                        }
                    }

                    // In case of a function call, we need to unwrap the BMLFunction type to get the return type
                    if (ctx.functionCall() != null) {
                        // If method: check parameters -> delegate check to BMLFunction
                        ((BMLFunction) resolvedType).checkParameters(ctx.functionCall());
                        ctx.type = ((BMLFunction) resolvedType).getReturnType();
                    } else {
                        ctx.type = resolvedType;
                    }
                }
                case "[" -> {
                    var firstExpression = ctx.expression().get(0);
                    var firstExpressionType = firstExpression.type;
                    var secondExpression = ctx.expression().get(1);
                    var secondExpressionType = secondExpression.type;

                    if (!(firstExpressionType instanceof BMLList)) {
                        throw new ParserException(EXPECTED_BUT_FOUND.format("List", firstExpressionType), firstExpression);
                    } else if (!(secondExpressionType instanceof BMLNumber)) {
                        throw new ParserException(EXPECTED_BUT_FOUND.format("Number", secondExpressionType), secondExpression);
                    } else if (!((BMLNumber) secondExpressionType).isFloatingPoint()) {
                        throw new ParserException(EXPECTED_BUT_FOUND.format(new BMLNumber(false), secondExpressionType), secondExpression);
                    }

                    // Safe cast because we checked that first expression is a list
                    ctx.type = ((BMLList) firstExpressionType).getItemType();
                }
                case "!" -> ctx.type = new BMLBoolean();
                case "<", "<=", ">", ">=" -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!(leftType instanceof BMLNumber)) {
                        throw new ParserException(EXPECTED_BUT_FOUND.format("Number", leftType), ctx.left);
                    } else if (!(rightType instanceof BMLNumber)) {
                        throw new ParserException(EXPECTED_BUT_FOUND.format("Number", rightType), ctx.right);
                    }

                    ctx.type = new BMLBoolean();
                }
                case "==", "!=" -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!leftType.equals(rightType)) {
                        throw new ParserException(INCOMPATIBLE.format(leftType, ctx.op.getText(), rightType), ctx);
                    }

                    ctx.type = new BMLBoolean();
                }
                case "+", "-", "*", "/", "%" -> {
                    if (ctx.left == null) {
                        var expressionType = ctx.expr.type;
                        if (!(expressionType instanceof BMLNumber)) {
                            throw new ParserException(EXPECTED_BUT_FOUND.format("Number", expressionType), ctx.expr);
                        }
                        ctx.type = new BMLNumber(((BMLNumber) expressionType).isFloatingPoint());
                    } else {
                        var leftType = ctx.left.type;
                        var rightType = ctx.right.type;
                        if (!(leftType instanceof BMLNumber)) {
                            throw new ParserException(EXPECTED_BUT_FOUND.format("Number", leftType), ctx.left);
                        } else if (!(rightType instanceof BMLNumber)) {
                            throw new ParserException(EXPECTED_BUT_FOUND.format("Number", rightType), ctx.right);
                        }

                        ctx.type = new BMLNumber(((BMLNumber) leftType).isFloatingPoint() || ((BMLNumber) rightType).isFloatingPoint());
                    }
                }
                case "and", "or" -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!(leftType instanceof BMLBoolean)) {
                        // TODO: Throw proper error
                        System.err.printf("left type %s is not boolean\n", leftType);
                        return;
                    } else if (!(rightType instanceof BMLBoolean)) {
                        System.err.printf("right type %s is not boolean\n", rightType);
                        return;
                    }
                    ctx.type = new BMLBoolean();
                }
                case "?" -> {
                    var condType = ctx.expression().get(0).type;
                    var firstType = ctx.expression().get(1).type;
                    var secondType = ctx.expression().get(2).type;
                    if (!(condType instanceof BMLBoolean)) {
                        System.err.printf("%s is not boolean\n", condType);
                        return;
                    } else if (!firstType.equals(secondType)) {
                        System.err.printf("type of first option %s is not the same as type of second option %s\n",
                                firstType, secondType);
                        return;
                    }

                    try {
                        ctx.type = ctx.expr.type.getClass().getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else { // Single function call
            var name = ctx.functionCall().functionName.getText();
            var symbol = currentScope.resolve(name);
            if (symbol == null) {
                throw new ParserException(NOT_DEFINED.format(name), ctx.functionCall().functionName);
            }

            ctx.type = ((BMLFunction) ((TypedSymbol) symbol).getType()).getReturnType();
        }
    }
}
