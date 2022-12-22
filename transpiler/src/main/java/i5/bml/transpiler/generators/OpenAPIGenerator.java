package i5.bml.transpiler.generators;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.openapi.BMLOpenAPIComponent;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

@CodeGenerator(typeClass = BMLOpenAPIComponent.class)
public class OpenAPIGenerator implements Generator {

    private final BMLOpenAPIComponent openAPIComponent;

    public OpenAPIGenerator(Type openAPIComponent) {
        this.openAPIComponent = (BMLOpenAPIComponent) openAPIComponent;
    }

    @Override
    public Node generateComponent(BMLParser.ComponentContext ctx, BMLBaseVisitor<Node> visitor) {
        var compilationUnit = new CompilationUnit();
        var container = compilationUnit.addClass("Container");

        openAPIComponent.getTagOperationIdPairs().keySet().stream().distinct().forEach(key -> {
            var tagOperationIdPair = openAPIComponent.getTagOperationIdPairs().get(key);
            var tag = StringUtils.capitalize(tagOperationIdPair.getLeft());
            var clientClassName = "%sApi".formatted(tag);
            var type = "ThreadLocal<%s>".formatted(clientClassName);
            var initializer = new MethodCallExpr(new NameExpr("ThreadLocal"), "withInitial",
                    new NodeList<>(new MethodReferenceExpr(new NameExpr(clientClassName), new NodeList<>(), "new")));
            FieldDeclaration field = container.addFieldWithInitializer(type, StringUtils.uncapitalize(clientClassName),
                    initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

            // TODO: Try to add import for %sApi. Maybe this method should class that already has a compilationUnit?
            //i5.bml.transpiler.bot.dialogue.DialogueAutomaton

            // Add getter & setter
            field.createGetter();
        });

        return compilationUnit;
    }

    @Override
    public Node generateFunctionCall(BMLParser.FunctionCallContext ctx, BMLBaseVisitor<Node> visitor) {
        var params = new ArrayList<>(((BMLFunctionType) ctx.type).getRequiredParameters());

        var pathParameter = params.stream()
                .filter(p -> p.getName().equals("path"))
                .findAny();

        //noinspection OptionalGetWithoutIsPresent -> Semantic analysis guarantees us presence of `path` parameter
        params.remove(pathParameter.get());
        var key = ctx.functionName.getText()
                + ((StringLiteralExpr) visitor.visit(pathParameter.get().getExprCtx())).asString();
        var tagOperationIdPair = openAPIComponent.getTagOperationIdPairs().get(key.toLowerCase());
        var tag = StringUtils.capitalize(tagOperationIdPair.getLeft());
        var operationId = tagOperationIdPair.getRight();
        String methodCall = "ComponentRegistry.get%sApi().%s".formatted(tag, operationId);

        params.addAll(((BMLFunctionType) ctx.type).getOptionalParameters());
        var args = params.stream()
                .map(p -> p.getExprCtx() == null ? new NullLiteralExpr() : (Expression) visitor.visit(p.getExprCtx()))
                .toArray(Expression[]::new);

        var catchClause = new CatchClause(new Parameter(StaticJavaParser.parseType("ApiException"), "e"), new BlockStmt());

        var tryBlock = new BlockStmt().addStatement(new MethodCallExpr(methodCall, args));

        return new TryStmt(tryBlock, new NodeList<>(catchClause), null);
    }
}
