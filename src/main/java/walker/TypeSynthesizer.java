package walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import types.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class TypeSynthesizer extends BMLBaseListener {

    private final Scope currentScope;

    public TypeSynthesizer(Scope currentScope) {
        this.currentScope = currentScope;
    }

    @Override
    public void exitComponent(BMLParser.ComponentContext ctx) {
        AbstractBMLType resolvedType = (AbstractBMLType) TypeRegistry.resolveType(ctx.type.getText());

        // Instantiate fields of `resolvedType` with component parameters
        for (var param : ctx.params.elementValuePair()) {
            // Check: Component instantiation parameter is defined by component type class
            var field = Arrays.stream(resolvedType.getClass().getDeclaredFields())
                    .filter(f -> param.name.getText().equals(f.getName()))
                    .findAny();

            if (field.isEmpty()) {
                // TODO: Proper error handling
                throw new IllegalStateException("Unknown field %s for component of type %s".formatted(param.name.getText(), resolvedType.getName()));
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

        // Invoke registered initializer methods of `resolvedType`
        Arrays.stream(resolvedType.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(InitializerMethod.class))
                .forEach(m -> {
                    try {
                        m.invoke(resolvedType);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Lastly, set `resolvedType` for symbol (we assume that it was created by the ST walker)
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



        } else { // Function invocation
            var functionInvocation = ctx.functionInvocation();
            var object = functionInvocation.object;
            var resolvedObjectSymbol = currentScope.resolve(object.getText());
            if (!(resolvedObjectSymbol instanceof VariableSymbol)) {
                throw new IllegalStateException("%s is not defined".formatted(object.getText()));
            }

            // Check: function is specified by the resolved type
            var resolvedObjectType = ((VariableSymbol) resolvedObjectSymbol).getType();
            var methods = resolvedObjectType.getClass().getDeclaredMethods();
            var resolvedFunction = Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(BMLFunction.class))
                    // We consider case for function names
                    .filter(m -> m.getName().equals(functionInvocation.functionName.getText()))
                    .findAny();

            if (resolvedFunction.isEmpty()) {
                throw new IllegalStateException("%s is not defined for object %s of type %s"
                        .formatted(functionInvocation.functionName.getText(), resolvedObjectSymbol.getName(),
                                ((VariableSymbol) resolvedObjectSymbol).getType().getName()));
            }

            // Check: parameter(s) specified by @BMLFunction are present and have correct type
            for (var requiredParameter : resolvedFunction.get().getParameters()) {
                // Name
                var name = requiredParameter.getAnnotation(BMLFunctionParameter.class).name();

                var invocationParameter = functionInvocation.elementExpressionPairList().elementExpressionPair()
                        .stream()
                        // We can assume that the BMLFunctionParameter annotation is present
                        // since it is checked by the type registry
                        // We consider case for the parameter name
                        .filter(p -> p.name.getText().equals(name))
                        .findAny();

                if (invocationParameter.isEmpty()) {
                    throw new IllegalStateException("Parameter %s is required but not present for function call %s"
                            .formatted(name, ctx.getText()));
                }

                // Type
                Type resolvedParameterType;
                try {
                    // This is okay since we check in the type registry
                    // that parameter types are of type org.antlr.symtab.Type
                    resolvedParameterType = (Type) requiredParameter.getType().getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    // TODO: Proper (internal) error message
                    throw new RuntimeException(e);
                }

                var invocationParameterType = invocationParameter.get().expression().type;
                if (!invocationParameterType.equals(resolvedParameterType)) {
                    throw new IllegalStateException("Parameter %s requires type %s but has %s"
                            .formatted(name, resolvedParameterType.getName(),
                                    invocationParameterType.getName()));
                }
            }

            // Invoke type-specific checks
            Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(BMLCheck.class))
                    .sorted(Comparator.comparingInt(m -> m.getAnnotation(BMLCheck.class).index()))
                    .forEach(m -> {
                        try {
                            m.invoke(resolvedObjectType, ctx.functionInvocation());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // Synthesize resulting type
            var synthesizer = Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(BMLSynthesizer.class))
                    .findAny();

            try {
                // We can assume that there exists at most one synthesizer function
                //noinspection OptionalGetWithoutIsPresent
                ctx.type = (Type) synthesizer.get().invoke(resolvedObjectType);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
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
