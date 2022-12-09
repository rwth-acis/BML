package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunction;
import i5.bml.parser.types.openapi.BMLOpenAPIComponent;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

@CodeGenerator(typeClass = BMLOpenAPIComponent.class)
public class OpenAPIGenerator implements Generator {

    BMLOpenAPIComponent openAPIComponent;


    public OpenAPIGenerator(Type openAPIComponent) {
        this.openAPIComponent = (BMLOpenAPIComponent) openAPIComponent;
    }

    @Override
    public Node generateComponent(BMLParser.ComponentContext componentContext, BMLBaseVisitor<Node> visitor) {
        var container = new ClassOrInterfaceDeclaration();

        openAPIComponent.getTagOperationIdPairs().keySet().stream().distinct().forEach(tag -> {
            var clientClassName = "%sApi".formatted(tag);
            var type = "ThreadLocal<%s>".formatted(clientClassName);
            var initializer = new MethodCallExpr(new NameExpr("ThreadLocal"), "withInitial",
                    new NodeList<>(new MethodReferenceExpr(new NameExpr(clientClassName), new NodeList<>(), "new")));
            FieldDeclaration field = container.addFieldWithInitializer(type, StringUtils.uncapitalize(clientClassName),
                    initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

            // TODO: Try to add import for %sApi. Maybe this method should class that already has a compilationUnit?

            // Add getter & setter
            container.addMember(field.createGetter());
        });

        return container;
    }

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        throw new UnsupportedOperationException(openAPIComponent.getName() + " does not support function calls");
    }

    @Override
    public Node generateFunctionCall(BMLParser.FunctionCallContext function, BMLBaseVisitor<Node> visitor) {
        var params = new ArrayList<>(((BMLFunction) function.type).getRequiredParameters());

        var pathParameter = params.stream()
                .filter(p -> p.getName().equals("path"))
                .findAny();

        //noinspection OptionalGetWithoutIsPresent -> Semantic analysis guarantees us presence of `path` parameter
        params.remove(pathParameter.get());
        var key = function.functionName.getText()
                + ((StringLiteralExpr) visitor.visit(pathParameter.get().getExprCtx())).asString();
        var tagOperationIdPair = openAPIComponent.getTagOperationIdPairs().get(key.toLowerCase());
        var tag = StringUtils.capitalize(tagOperationIdPair.getLeft());
        var operationId = tagOperationIdPair.getRight();
        String methodCall = "ComponentRegistry.get%sApi().%s".formatted(tag, operationId);

        params.addAll(((BMLFunction) function.type).getOptionalParameters());
        var args = params.stream()
                .map(p -> p.getExprCtx() == null ? new NullLiteralExpr() : (Expression) visitor.visit(p.getExprCtx()))
                .toArray(Expression[]::new);

        return new MethodCallExpr(methodCall, args);
    }
}
