package i5.bml.transpiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;

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
        return super.visitBlock(ctx);
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
        var type = StaticJavaParser.parseClassOrInterfaceType(ctx.expr.type.toString());
        var v = new VariableDeclarator(type, ctx.name.getText(), (Expression) visit(ctx.expr));
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
                    if (ctx.Identifier() != null) {
                        // TODO: Do the same as for function calls, decide what to do based on the type
                        //       if OpenAPI -> turn `pet.id` into `pet.getId()`
                        yield new FieldAccessExpr((Expression) visit(ctx.expr), ctx.Identifier().getText());
                    } else { // functionCall
                        yield visit(ctx.functionCall());
                    }
                }

                case BMLParser.LBRACK -> new MethodCallExpr((Expression) visit(ctx.expr), new SimpleName("get"),
                        new NodeList<>((Expression) visit(ctx.index)));

                case BMLParser.BANG -> new UnaryExpr((Expression) visit(ctx.expr), UnaryExpr.Operator.LOGICAL_COMPLEMENT);

                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE, BMLParser.EQUAL, BMLParser.NOTEQUAL,
                        BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD ->
                        new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                                BinaryExpr.Operator.valueOf(ctx.op.getText()));

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

        return super.visitAtom(ctx);
    }

    @Override
    public Node visitFunctionCall(BMLParser.FunctionCallContext ctx) {
        // TODO: if OpenAPI -> turn `petStore.get(path="/pet/{petId}", petId=1)` into `petApi.getPetById(1)`
        //       `getPetById` is the operation id of GET /pet/{petId}
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
