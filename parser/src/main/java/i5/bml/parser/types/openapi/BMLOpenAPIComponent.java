package i5.bml.parser.types.openapi;

import generatedParser.BMLParser;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import i5.bml.parser.types.*;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.*;
import java.util.stream.Collectors;

import static i5.bml.parser.errors.ParserError.*;

@BMLType(name = "OpenAPI", isComplex = true)
public class BMLOpenAPIComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "url", expectedBMLType = "String", isRequired = true)
    private String url;

    private OpenAPI openAPI;

    private Set<String> routes;

    private final Set<String> httpMethods = new HashSet<>();

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        var expr = ctx.elementExpressionPair(0).expr;
        var atom = expr.atom();
        if (atom == null || atom.literal() == null) {
            diagnosticsCollector.addDiagnostic(EXPECTED_BUT_FOUND.format("String", expr.type), expr);
        } else {
            url = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }

    @Override
    public void initializeType(DiagnosticsCollector diagnosticsCollector, ParserRuleContext ctx) {
        Objects.requireNonNull(url);

        var start = System.nanoTime();
        SwaggerParseResult result = new OpenAPIParser().readLocation(url, null, null);
        var end = System.nanoTime();
        Measurements.add("Fetch OpenAPI Spec", (end - start));

        // Check for OpenAPI parser errors
        if (result.getMessages() != null && result.getMessages().size() > 0) {
            diagnosticsCollector.addDiagnostic("Could not connect to url `%s`\nPossible reason(s):\n%s"
                    .formatted(url, String.join("\n", result.getMessages())), ctx);
            return;
        }

        openAPI = result.getOpenAPI();

        // Set valid OpenAPI routes
        routes = openAPI.getPaths().keySet();

        // Determine route return types & arguments
        start = System.nanoTime();
        openAPI.getPaths().forEach((route, value) -> value.readOperationsMap().forEach((httpMethod, operation) -> {
            AbstractBMLType returnType = (AbstractBMLType) computeRouteReturnTypes(operation, diagnosticsCollector, ctx);

            var requiredParameters = computeRouteArguments(operation, true, diagnosticsCollector, ctx);
            var optionalParameters = computeRouteArguments(operation, false, diagnosticsCollector, ctx);

            var p = new ParameterSymbol("path");
            p.setType(TypeRegistry.resolveType("String"));
            requiredParameters.add(p);

            var function = new BMLFunction(returnType, requiredParameters, optionalParameters);
            supportedAccesses.put(httpMethod.name().toLowerCase() + route, function);
            httpMethods.add(httpMethod.name().toLowerCase());
        }));
        end = System.nanoTime();
        Measurements.add("Parsing OpenAPI Spec", end - start);
    }

    private Type computeRouteReturnTypes(Operation operation, DiagnosticsCollector diagnosticsCollector, ParserRuleContext ctx) {
        if (operation.getResponses() == null) {
            var msg = "Operation `%s` has no response definition".formatted(operation.getOperationId());
            diagnosticsCollector.addDiagnostic(msg, ctx, DiagnosticSeverity.Error);
            return null;
        }

        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            String code = entry.getKey();
            ApiResponse response = entry.getValue();
            if (response.getContent() != null) {
                // TODO: ATM we only support application/json
                var mediaType = response.getContent().get("application/json");
                if (mediaType == null) {
                    var msg = "Operation `%s` has no application/json media type for response code `%s`\n".formatted(operation.getOperationId(), code);
                    diagnosticsCollector.addDiagnostic(msg, ctx, DiagnosticSeverity.Warning);
                } else {
                    var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(mediaType.getSchema(),
                            "Operation", operation.getOperationId());
                    return BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
                }
            }
        }

        return null; // TODO: We need a void type
    }

    private void computeArgumentTypes(List<ParameterSymbol> arguments, Schema<?> schema, String parameterName,
                                      DiagnosticsCollector diagnosticsCollector, ParserRuleContext ctx) {
        if (schema == null) {
            var msg = "Couldn't find schema for parameter `%s`".formatted(parameterName);
            diagnosticsCollector.addDiagnostic(msg, ctx, DiagnosticSeverity.Warning);
            return;
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);

        var parameterSymbol = new ParameterSymbol(parameterName);
        parameterSymbol.setType(resolvedBMLType);
        arguments.add(parameterSymbol);
    }

    private List<ParameterSymbol> computeRouteArguments(Operation operation, boolean collectRequired,
                                                        DiagnosticsCollector diagnosticsCollector, ParserRuleContext ctx) {
        var arguments = new ArrayList<ParameterSymbol>();
        if (operation.getParameters() != null && operation.getParameters().size() > 0) {
            operation.getParameters().stream()
                    .filter(p -> p.getRequired() == collectRequired)
                    .forEach(p -> computeArgumentTypes(arguments, p.getSchema(), p.getName(), diagnosticsCollector, ctx));
        }

        // We can have both parameters and a request body
        if (operation.getRequestBody() != null) {
            if (operation.getRequestBody().getContent() == null) {
                var msg = "Could not find content for request body of operation %s".formatted(operation.getOperationId());
                diagnosticsCollector.addDiagnostic(msg, ctx, DiagnosticSeverity.Error);
                return arguments;
            }

            // TODO: ATM we only support application/json
            var mediaType = operation.getRequestBody().getContent().get("application/json");
            if (mediaType == null) {
                // TODO: for now we ignore other media types than application/json
                var msg = "Operation `%s` has no application/json media type".formatted(operation.getOperationId());
                diagnosticsCollector.addDiagnostic(msg, ctx, DiagnosticSeverity.Warning);
            } else {
                var r = operation.getRequestBody().getRequired();
                if ((r == null && !collectRequired) || (r != null && r == collectRequired)) {
                    computeArgumentTypes(arguments, mediaType.getSchema(), "body", diagnosticsCollector, ctx);
                }
            }
        }

        return arguments;
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        var start = System.nanoTime();
        var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
        var httpMethod = functionCallCtx.functionName.getText();

        // Check: http method is valid
        if (!httpMethods.contains(httpMethod)) {
            diagnosticsCollector.addDiagnostic(NOT_DEFINED.format(httpMethod), functionCallCtx.functionName);
            return TypeRegistry.resolveType("Object");
        }

        // Check: path is specified
        var pathParameter = functionCallCtx.elementExpressionPairList().elementExpressionPair().stream()
                .filter(p -> p.name.getText().equals("path"))
                .findAny();

        if (pathParameter.isEmpty()) {
            diagnosticsCollector.addDiagnostic(MISSING_PARAM.format("path"), functionCallCtx);
            return TypeRegistry.resolveType("Object");
        }

        // Check: path parameter has correct type
        var pathParameterType = pathParameter.get().expression().type;
        if (!pathParameterType.equals(TypeRegistry.resolveType("String"))) {
            diagnosticsCollector.addDiagnostic(EXPECTED_BUT_FOUND.format("String", pathParameterType),
                    pathParameter.get().expression());
            return TypeRegistry.resolveType("Object");
        }

        // Check: route is valid
        var path = pathParameter.get().expression().getText();
        path = path.substring(1, path.length() - 1);

        if (!routes.contains(path)) {
            diagnosticsCollector.addDiagnostic("Path `%s` is not defined for API:\n`%s`"
                    .formatted(path, url), pathParameter.get().expression());
            return TypeRegistry.resolveType("Object");
        }

        // Check: HTTP_METHOD + ROUTE is a valid combination
        var functionType = supportedAccesses.get(httpMethod + path);

        if (functionType == null) {
            diagnosticsCollector.addDiagnostic("Path `%s` does not support HTTP method `%s` for API:\n`%s`"
                            .formatted(path, httpMethod, url), pathParameter.get().expression());
            return TypeRegistry.resolveType("Object");
        }

        var end = System.nanoTime();
        Measurements.add("Resolve & check call `%s`".formatted(functionCallCtx.functionName.getText()), end - start);
        return functionType;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BMLOpenAPIComponent that = (BMLOpenAPIComponent) o;

        return this.getName().equals(that.getName()) && url.equals(that.getUrl());
    }
}
