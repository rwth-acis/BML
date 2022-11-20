package walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.antlr.symtab.Symbol;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import types.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Arrays;

public class TypeSynthesizer extends AbstractScope {

    @Override
    public void exitComponent(BMLParser.ComponentContext ctx) {
        AbstractBMLType resolvedType = (AbstractBMLType) TypeRegistry.resolveType(ctx.type.getText());

        // Instantiate type parameter
        for (var param : ctx.params.elementValuePair()) {
            var field = Arrays.stream(resolvedType.getClass().getDeclaredFields())
                    .filter(f -> param.name.getText().equals(f.getName()))
                    .findAny();

            if (field.isEmpty()) {
                // TODO: Proper error handling
                throw new IllegalStateException("Unknown field %s for component of type %s".formatted(param.name.getText(), resolvedType.getName()));
            }

            var resolvedSymbol = currentScope.resolve(ctx.name.getText());
            if (!(resolvedSymbol instanceof VariableSymbol)) {
                // TODO: Proper error handling
                throw new IllegalStateException("%s is not defined".formatted(param.name.getText()));
            }

            var canAccess = field.get().canAccess(resolvedType);
            field.get().setAccessible(true);

            var terminalNodeType = ((TerminalNode) param.value.literal().getChild(0)).getSymbol().getType();
            var literalInstance = switch (terminalNodeType) {
                case BMLParser.IntegerLiteral, BMLParser.FloatingPointLiteral -> new BigDecimal(param.value.getText());
                case BMLParser.StringLiteral -> param.value.getText().substring(1, param.value.getText().length() - 1);
                case BMLParser.BooleanLiteral -> Boolean.parseBoolean(param.value.getText());
                default -> // TODO: Proper error message
                        throw new IllegalStateException("Unexpected type: " + terminalNodeType);
            };

            try {
                field.get().set(resolvedType, literalInstance);
            } catch (IllegalAccessException e) {
                // TODO
                throw new RuntimeException(e);
            }

            field.get().setAccessible(canAccess);
        }

        // Invoke registered initializer methods
        Arrays.stream(resolvedType.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(InitializerMethod.class))
                .forEach(m -> {
                    try {
                        m.invoke(resolvedType);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void exitAssignment(BMLParser.AssignmentContext ctx) {
        // When exiting an assignment, we can assume that the right-hand side type was computed already,
        // since it is an expression
        if (ctx.op.getText().equals("=")) {
            VariableSymbol v = new VariableSymbol(ctx.name.getText());
            v.setType(ctx.expression().type);
            currentScope.define(v);
        } else {
            // Make sure left-hand side is declared
            var v = currentScope.resolve(ctx.name.getText());
            if (!(v instanceof VariableSymbol)) {
                // TODO: Proper error handling
                throw new IllegalStateException("%s is not defined".formatted(ctx.name.getText()));
            }

            var leftType = ((VariableSymbol) v).getType();
            var rightType = ctx.expression().type;
            if (!(leftType instanceof BMLNumeric)) {
                // TODO: Throw proper error
                System.err.printf("left type %s is not numeric\n", leftType);
                return;
            } else if (!(rightType instanceof BMLNumeric)) {
                System.err.printf("right type %s is not numeric\n", rightType);
                return;
            }
        }
    }

    @Override
    public void exitLiteral(BMLParser.LiteralContext ctx) {
        var terminalNodeType = ((TerminalNode) ctx.getChild(0)).getSymbol().getType();
        ctx.type = switch (terminalNodeType) {
            case BMLParser.IntegerLiteral, BMLParser.FloatingPointLiteral -> new BMLNumeric();
            case BMLParser.StringLiteral -> new BMLString();
            case BMLParser.BooleanLiteral -> new BMLBoolean();
            default -> {
                // TODO: Proper error message
                throw new IllegalStateException("Unexpected type: " + terminalNodeType);
            }
        };
    }

    @Override
    public void exitAtom(BMLParser.AtomContext ctx) {
        if (ctx.literal() != null) {
            ctx.type = ctx.literal().type;
        } else if (ctx.Identifier() != null) {
            var r = currentScope.resolve(ctx.Identifier().getText());
            if (!(r instanceof VariableSymbol)) {
                // TODO: Throw proper error
                System.err.printf("%s is not defined%n", ctx.Identifier().getText());
                return;
            }

            ctx.type = ((VariableSymbol) r).getType();
        } else if (ctx.objectAccess() != null) {
            var object = ctx.objectAccess().object;
            var resolvedObjectType = currentScope.resolve(object.getText());
            if (!(resolvedObjectType instanceof VariableSymbol)) {
                throw new IllegalStateException("%s is not defined".formatted(object.getText()));
            }

            var fieldAccesses = ctx.objectAccess().Identifier();
            for (int i = 0; i < fieldAccesses.size(); i++) {

            }
        } else { // Function invocation
            // TODO
        }
    }

    @Override
    public void exitExpression(BMLParser.ExpressionContext ctx) {
        var childCount = ctx.getChildCount();
        if (childCount > 1) {
            switch (ctx.op.getText()) {
                case "(" -> {
                    try {
                        ctx.type = ctx.expr.type.getClass().getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        // TODO: Proper error handling
                        throw new RuntimeException(e);
                    }
                }
                case "!" -> ctx.type = new BMLBoolean();
                case "<", "<=", ">", ">=" -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!(leftType instanceof BMLNumeric)) {
                        // TODO: Throw proper error
                        System.err.printf("left type %s is not numeric\n", leftType);
                        return;
                    } else if (!(rightType instanceof BMLNumeric)) {
                        System.err.printf("right type %s is not numeric\n", rightType);
                        return;
                    }

                    ctx.type = new BMLBoolean();
                }
                case "==", "!=" -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!leftType.equals(rightType)) {
                        // TODO: Throw proper error
                        //System.out.println(ctx.start.getLine());
                        //System.out.println(ctx.start.getCharPositionInLine());
                        System.err.printf("left type %s and right type %s are not compatible\n", leftType, rightType);
                        return;
                    }

                    ctx.type = new BMLBoolean();
                }
                case "+", "-", "*", "/", "%" -> {
                    if (ctx.left == null) {
                        if (!(ctx.expr.type instanceof BMLNumeric)) {
                            // TODO: Throw proper error
                            System.err.printf("type %s is not numeric\n", ctx.expr.type);
                            return;
                        }
                    } else {
                        var leftType = ctx.left.type;
                        var rightType = ctx.right.type;
                        if (!(leftType instanceof BMLNumeric)) {
                            // TODO: Throw proper error
                            System.err.printf("left type %s is not numeric\n", leftType);
                            return;
                        } else if (!(rightType instanceof BMLNumeric)) {
                            System.err.printf("right type %s is not numeric\n", rightType);
                            return;
                        }
                    }
                    ctx.type = new BMLNumeric();
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
        } else if (childCount == 1) { // We have an atom
            ctx.type = ctx.atom().type;
        }
    }
}
