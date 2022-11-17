package walker;

import generatedParser.BMLParser;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import types.BMLBoolean;
import types.BMLNumeric;
import types.BMLString;

import java.lang.reflect.InvocationTargetException;

public class TypeSynthesizer extends SymbolTableAndScopeGenerator {

    @Override
    public void exitComponent(BMLParser.ComponentContext ctx) {

    }

    @Override
    public void exitAssignment(BMLParser.AssignmentContext ctx) {
        switch (ctx.op.getText()) {
            case "=" -> {
                VariableSymbol v = new VariableSymbol(ctx.name.getText());
                // When exiting an assignment, we can assume that the right-hand side type was computed already,
                // since it is either an expression or a function invocation
                if (ctx.expression() != null) {
                    v.setType(ctx.expression().type);
                } else { // We have function invocation
                    v.setType(ctx.functionInvocation().type);
                }
                currentScope.define(v);
            }
            default -> {
                // Make sure left-hand side is declared
                var v = currentScope.resolve(ctx.name.getText());
                if (!(v instanceof VariableSymbol)) {
                    // TODO: Proper error handling
                    throw new IllegalStateException("%s is not defined".formatted(ctx.name.getText()));
                }

                var leftType = ((VariableSymbol) v).getType();
                var rightType = ctx.expression() != null ? ctx.expression().type : ctx.functionInvocation().type;
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
        } else { // object access
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
