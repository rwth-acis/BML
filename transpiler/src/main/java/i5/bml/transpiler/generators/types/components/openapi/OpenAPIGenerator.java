package i5.bml.transpiler.generators.types.components.openapi;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import generatedParser.BMLParser;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.openapi.BMLOpenAPIComponent;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.OpenAPIUtils;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

@CodeGenerator(typeClass = BMLOpenAPIComponent.class)
public class OpenAPIGenerator extends Generator {

    private final BMLOpenAPIComponent openAPIComponent;

    private String apiName;

    public OpenAPIGenerator(Type openAPIComponent) {
        this.openAPIComponent = (BMLOpenAPIComponent) openAPIComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        apiName = ctx.name.getText() + "client";
        openAPIComponent.apiName(apiName);

        // Make sure that dependencies and included in gradle build file
        visitor.gradleFile().add("hasOpenAPIComponent", true);

        // Generate swagger client code
        // TODO
        //OpenAPIUtils.generateOpenAPIClientCode(openAPIComponent.openAPISpec(), visitor.outputPackage(), apiName, visitor.botOutputPath());
        OpenAPIUtils.generateOpenAPIClientCode(openAPIComponent.url(), visitor.outputPackage(), apiName, visitor.botOutputPath());

        // Generate fields with getters in `ComponentRegistry`
        openAPIComponent.tagOperationIdPairs().values().stream().map(Pair::getLeft).distinct().forEach(tag -> {
            var clientClassName = "%sApi".formatted(StringUtils.capitalize(tag));

            // Add field for API
            var type = "ThreadLocal<%s>".formatted(clientClassName);
            var initializer = new MethodCallExpr(new NameExpr("ThreadLocal"), "withInitial",
                    new NodeList<>(new MethodReferenceExpr(new NameExpr(clientClassName), new NodeList<>(), "new")));
            FieldDeclaration field = visitor.currentClass().addFieldWithInitializer(type, StringUtils.uncapitalize(clientClassName),
                    initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

            // Add import for %sApi
            var packageName = getAPIImport(visitor, clientClassName);
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            visitor.currentClass().findCompilationUnit().get().addImport(packageName, false, false);

            // Add getter
            var getter = field.createGetter();
            getter.addModifier(Modifier.Keyword.STATIC);
            getter.setType(clientClassName);
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence, since we just generated it
            var returnStmt = getter.getBody().get().getStatement(0).asReturnStmt();
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence, since we just generated it
            var expr = returnStmt.getExpression().get();
            returnStmt.setExpression(new MethodCallExpr(expr, "get", new NodeList<>()));
        });
    }

    private String getAPIImport(JavaTreeGenerator visitor, String clientClassName) {
        // Add import for %sApi
        if (!visitor.outputPackage().isEmpty()) {
            return visitor.outputPackage() + ".openapi." + apiName + ".apis." + clientClassName;
        } else {
            return "openapi." + apiName + ".apis." + clientClassName;
        }
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        // Method call
        var params = new ArrayList<>(((BMLFunctionType) ctx.type).getRequiredParameters());
        var pathParameter = params.stream()
                .filter(p -> p.getName().equals("path"))
                .findAny();
        //noinspection OptionalGetWithoutIsPresent -> Semantic analysis guarantees us presence of `path` parameter
        params.remove(pathParameter.get());

        var key = ctx.functionName.getText()
                + ((StringLiteralExpr) visitor.visit(pathParameter.get().exprCtx())).asString();
        var tagOperationIdPair = openAPIComponent.tagOperationIdPairs().get(key.toLowerCase());
        var tag = StringUtils.capitalize(tagOperationIdPair.getLeft());
        var operationId = tagOperationIdPair.getRight();
        String methodCall = "ComponentRegistry.get%sApi().%s".formatted(tag, operationId);
        params.addAll(((BMLFunctionType) ctx.type).getOptionalParameters());
        var args = params.stream()
                .map(p -> {
                    if (p.exprCtx() == null) {
                        return new NullLiteralExpr();
                    } else {
                        var node = visitor.visit(p.exprCtx());
                        if (p.getType().equals(TypeRegistry.resolveType(BuiltinType.LONG_NUMBER))) {
                            return new CastExpr(PrimitiveType.longType(), (Expression) node);
                        } else {
                            return (Expression) node;
                        }
                    }
                })
                .toArray(Expression[]::new);
        var methodCallExpr = new MethodCallExpr(methodCall, args);

        // Import for `ComponentRegistry`
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        var compilationUnit = visitor.currentClass().findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);

        // Import for `ApiException`
        var packageName = getOpenAPIImport(visitor);
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        visitor.currentClass().findCompilationUnit().get().addImport(packageName, false, false);

        // Inform assignment visitor about try stmt wrapping
        visitor.wrapAssignmentInTryStmt(true);

        return methodCallExpr;
    }

    private String getOpenAPIImport(JavaTreeGenerator visitor) {
        // Add import for %sApi
        if (!visitor.outputPackage().isEmpty()) {
            return visitor.outputPackage() + ".openapi." + apiName + "." + "ApiException";
        } else {
            return "openapi." + apiName + "." + "ApiException";
        }
    }
}
