package i5.bml.transpiler;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaSynthesizer extends BMLBaseVisitor<Node> {

    @Override
    public Node visitProgram(BMLParser.ProgramContext ctx) {
        return super.visitProgram(ctx);
    }

    @Override
    public Node visitBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        return super.visitBotDeclaration(ctx);
    }

    @Override
    public Node visitBotHead(BMLParser.BotHeadContext ctx) {
        return super.visitBotHead(ctx);
    }

    @Override
    public Node visitBotBody(BMLParser.BotBodyContext ctx) {
        return super.visitBotBody(ctx);
    }

    @Override
    public Node visitElementExpressionPairList(BMLParser.ElementExpressionPairListContext ctx) {
        return super.visitElementExpressionPairList(ctx);
    }

    @Override
    public Node visitElementExpressionPair(BMLParser.ElementExpressionPairContext ctx) {
        return super.visitElementExpressionPair(ctx);
    }

    @Override
    public Node visitComponent(BMLParser.ComponentContext ctx) {
        return super.visitComponent(ctx);
    }

    @Override
    public Node visitFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        return super.visitFunctionDefinition(ctx);
    }

    @Override
    public Node visitAnnotation(BMLParser.AnnotationContext ctx) {
        return super.visitAnnotation(ctx);
    }

    @Override
    public Node visitFunctionHead(BMLParser.FunctionHeadContext ctx) {
        return super.visitFunctionHead(ctx);
    }

    @Override
    public Node visitStatement(BMLParser.StatementContext ctx) {
        return super.visitStatement(ctx);
    }

    @Override
    public Node visitBlock(BMLParser.BlockContext ctx) {
        var b = new BlockStmt(ctx.statement().stream()
                .map(p -> {
                    var v = visit(p);
                    System.out.println("VISITED: " + p.getText());
                    if (!(v instanceof Statement)) {
                        return new ExpressionStmt((Expression) v);
                    } else {
                        return (Statement) v;
                    }
                })
                .collect(Collectors.toCollection(NodeList::new))
        );
        System.out.println(b);
        return b;
    }

    @Override
    public Node visitIfStatement(BMLParser.IfStatementContext ctx) {
        return super.visitIfStatement(ctx);
    }

    @Override
    public Node visitForEachStatement(BMLParser.ForEachStatementContext ctx) {
        return super.visitForEachStatement(ctx);
    }

    @Override
    public Node visitForEachBody(BMLParser.ForEachBodyContext ctx) {
        return super.visitForEachBody(ctx);
    }

    @Override
    public Node visitAssignment(BMLParser.AssignmentContext ctx) {
        var type = BMLTypeResolver.resolveBMLTypeToJavaType(ctx.expr.type);
        var v = new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(type, ctx.name.getText(), (Expression) visit(ctx.expr))));
        return v;
    }

    @Override
    public Node visitExpression(BMLParser.ExpressionContext ctx) {
        if (ctx.atom() != null) {
            return visit(ctx.atom());
        } else if (ctx.op != null) {
            return switch (ctx.op.getType()) {
                case BMLParser.LBRACE -> new EnclosedExpr((Expression) visit(ctx.expr));

                case BMLParser.DOT -> {
                    Generator generator = GeneratorRegistry.getGeneratorForType(ctx.expr.type);
                    if (ctx.Identifier() != null) {
                        yield generator.generateFieldAccess((Expression) visit(ctx.expr), ctx.Identifier());
                    } else { // functionCall
                        yield generator.generateFunctionCall(ctx.functionCall(), this);
                    }
                }

                case BMLParser.LBRACK -> new MethodCallExpr((Expression) visit(ctx.expr), new SimpleName("get"),
                        new NodeList<>((Expression) visit(ctx.index)));

                case BMLParser.BANG -> new UnaryExpr((Expression) visit(ctx.expr), UnaryExpr.Operator.LOGICAL_COMPLEMENT);

                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE, BMLParser.EQUAL, BMLParser.NOTEQUAL,
                        BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD ->
                        new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                                Arrays.stream(BinaryExpr.Operator.values())
                                        .filter(op -> op.asString().equals(ctx.op.getText())).findAny().get()
                        );

                case BMLParser.AND -> new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                        BinaryExpr.Operator.AND);

                case BMLParser.OR -> new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                        BinaryExpr.Operator.OR);

                case BMLParser.QUESTION -> new ConditionalExpr((Expression) visit(ctx.cond), (Expression) visit(ctx.thenExpr),
                        (Expression) visit(ctx.elseExpr));

                // This should never happen
                default -> throw new IllegalStateException("Unexpected ctx.op: %s\nContext: %s".formatted(ctx.op, ctx));
            };
        } else if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        } else { // Initializers
            return visit(ctx.initializer());
        }
    }

    @Override
    public Node visitAtom(BMLParser.AtomContext ctx) {
        return switch (ctx.token.getType()) {
            case BMLParser.IntegerLiteral -> new LongLiteralExpr(ctx.token.getText() + "L");
            case BMLParser.FloatingPointLiteral -> new DoubleLiteralExpr(ctx.token.getText() + "d");
            case BMLParser.StringLiteral -> new StringLiteralExpr(ctx.token.getText().substring(1, ctx.token.getText().length() - 1));
            case BMLParser.BooleanLiteral -> new BooleanLiteralExpr(Boolean.parseBoolean(ctx.token.getText()));
            case BMLParser.Identifier -> new NameExpr(ctx.token.getText());
            // This should never happen
            default -> throw new IllegalStateException("Unknown token was parsed: %s\nContext: %s".formatted(ctx.getText(), ctx));
        };
    }

    @Override
    public Node visitFunctionCall(BMLParser.FunctionCallContext ctx) {
        // TODO: These calls can only be STDLIB calls
        return super.visitFunctionCall(ctx);
    }

    @Override
    public Node visitMapInitializer(BMLParser.MapInitializerContext ctx) {
        var elementExpressionPairList = ctx.elementExpressionPairList();
        if (elementExpressionPairList != null) {
            var arguments = elementExpressionPairList.elementExpressionPair().stream()
                    .flatMap(p -> Stream.of(new StringLiteralExpr(p.name.getText()), (Expression) visit(p.expr)))
                    .collect(Collectors.toCollection(NodeList::new));
            return new MethodCallExpr(new NameExpr("Map"), new SimpleName("of"), arguments);
        } else {
            return new MethodCallExpr(new NameExpr("Map"), new SimpleName("of"));
        }
    }

    @Override
    public Node visitListInitializer(BMLParser.ListInitializerContext ctx) {
        var arguments = ctx.expression().stream()
                .map(e -> (Expression) visit(e))
                .collect(Collectors.toCollection(NodeList::new));
        return new MethodCallExpr(new NameExpr("List"), new SimpleName("of"), arguments);
    }

    @Override
    public Node visitDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        return super.visitDialogueAutomaton(ctx);
    }

    @Override
    public Node visitDialogueHead(BMLParser.DialogueHeadContext ctx) {
        return super.visitDialogueHead(ctx);
    }

    @Override
    public Node visitDialogueBody(BMLParser.DialogueBodyContext ctx) {
        return super.visitDialogueBody(ctx);
    }

    @Override
    public Node visitAutomatonTransitions(BMLParser.AutomatonTransitionsContext ctx) {
        return super.visitAutomatonTransitions(ctx);
    }

    @Override
    public Node visitTransitionInitializer(BMLParser.TransitionInitializerContext ctx) {
        return super.visitTransitionInitializer(ctx);
    }
}
