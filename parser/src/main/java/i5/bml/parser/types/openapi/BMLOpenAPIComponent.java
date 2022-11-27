package i5.bml.parser.types.openapi;

import generatedParser.BMLParser;
import i5.bml.parser.errors.ParserError;
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
import org.antlr.v4.runtime.tree.ParseTree;
import i5.bml.parser.types.*;

import javax.lang.model.type.ErrorType;
import java.util.*;

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
            diagnosticsCollector.addDiagnostic(ParserError.EXPECTED_BUT_FOUND.format("String", expr.type), expr);
        } else {
            url = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }

    @Override
    public void initializeType() {
        Objects.requireNonNull(url);

        var start = System.nanoTime();
        SwaggerParseResult result = new OpenAPIParser().readLocation(url, null, null);
        var end = System.nanoTime();
        Measurements.add("Fetch OpenAPI Spec", (end - start));
        openAPI = result.getOpenAPI();

        // Check for OpenAPI i5.bml.parser.Parser i5.bml.parser.errors
        if (result.getMessages() != null && result.getMessages().size() > 0) {
            StringBuilder s = new StringBuilder();
            s.append("Encountered error messages for url '%s':\n".formatted(url));
            result.getMessages().forEach(m -> {
                s.append(m).append("\n");
            });

            // TODO: Proper error handling
            throw new IllegalStateException(s.toString());
        } else if (openAPI == null) {
            throw new IllegalStateException("Could not connect to url '%s'".formatted(url));
        }

        // Set valid OpenAPI routes
        routes = openAPI.getPaths().keySet();

        // Determine route return types & arguments
        start = System.nanoTime();
        openAPI.getPaths().forEach((route, value) -> value.readOperationsMap().forEach((httpMethod, operation) -> {
            AbstractBMLType returnType = (AbstractBMLType) computeRouteReturnTypes(operation);

            var requiredParameters = computeRouteArguments(operation, true);
            var optionalParameters = computeRouteArguments(operation, false);

            var p = new ParameterSymbol("path");
            p.setType(TypeRegistry.resolvePrimitiveType("String"));
            requiredParameters.add(p);

            var function = new BMLFunction(returnType, requiredParameters, optionalParameters);
            supportedAccesses.put(httpMethod.name().toLowerCase() + route, function);
            httpMethods.add(httpMethod.name().toLowerCase());
        }));
        end = System.nanoTime();
        Measurements.add("Parsing OpenAPI Spec", end - start);
    }

    private Type computeRouteReturnTypes(Operation operation) {
        if (operation.getResponses() == null) {
            System.err.printf("Operation `%s` has no response definition\n", operation.getOperationId());
            return null;
        }

        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            String code = entry.getKey();
            ApiResponse response = entry.getValue();
            if (response.getContent() != null) {
                // TODO: ATM we only support application/json
                var mediaType = response.getContent().get("application/json");
                if (mediaType == null) {
                    System.err.printf("Operation `%s` has no application/json media type for code %s\n", operation.getOperationId(), code);
                } else {
                    var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(mediaType.getSchema(),
                            "Operation", operation.getOperationId());
                    return BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
                }
            }
        }

        return null; // TODO: We need a void type
    }

    private void computeArgumentTypes(List<ParameterSymbol> arguments, Schema<?> schema, String parameterName) {
        if (schema == null) {
            throw new IllegalStateException("Couldn't find schema for parameter %s"
                    .formatted(parameterName));
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);

        var parameterSymbol = new ParameterSymbol(parameterName);
        parameterSymbol.setType(resolvedBMLType);
        arguments.add(parameterSymbol);
    }

    private List<ParameterSymbol> computeRouteArguments(Operation operation, boolean collectRequired) {
        var arguments = new ArrayList<ParameterSymbol>();
        if (operation.getParameters() != null && operation.getParameters().size() > 0) {
            operation.getParameters().stream()
                    .filter(p -> p.getRequired() == collectRequired)
                    .forEach(p -> computeArgumentTypes(arguments, p.getSchema(), p.getName()));
        }

        // We can have both parameters and a request body
        if (operation.getRequestBody() != null) {
            if (operation.getRequestBody().getContent() == null) {
                // TODO
                throw new IllegalStateException("Could not find content for request body of operation %s"
                        .formatted(operation.getOperationId()));
            }

            // TODO: ATM we only support application/json
            var mediaType = operation.getRequestBody().getContent().get("application/json");
            if (mediaType == null) {
                // TODO: for now we ignore other media i5.bml.parser.types than application/json
                System.err.printf("Operation `%s` has no application/json media type\n", operation.getOperationId());
            } else {
                var r = operation.getRequestBody().getRequired();
                if ((r == null && !collectRequired) || (r != null && r == collectRequired)) {
                    computeArgumentTypes(arguments, mediaType.getSchema(), "body");
                }
            }
        }

        return arguments;
    }

    @Override
    public Type resolveAccess(ParseTree ctx) {
        var start = System.nanoTime();
        var functionCtx = (BMLParser.FunctionCallContext) ctx;
        var httpMethod = functionCtx.functionName.getText();

        // Check: http method is valid
        if (!httpMethods.contains(httpMethod)) {
            throw new IllegalStateException("Unknown function %s".formatted(httpMethod));
        }

        // Check: path is specified
        var pathParameter = functionCtx.elementExpressionPairList().elementExpressionPair().stream()
                .filter(p -> p.name.getText().equals("path"))
                .findAny();

        if (pathParameter.isEmpty()) {
            throw new IllegalStateException("Missing parameter `path` for function call %s".formatted(httpMethod));
        }

        // Check: path parameter has correct type
        Type bmlString = new BMLString();
        var pathParameterType = pathParameter.get().expression().type;
        if (!pathParameterType.equals(bmlString)) {
            throw new IllegalStateException("Expected type %s for parameter `path`, but found %s".formatted(bmlString, pathParameterType));
        }

        // Check: route is valid
        var path = pathParameter.get().expression().getText();
        path = path.substring(1, path.length() - 1);

        if (!routes.contains(path)) {
            throw new IllegalStateException("Unknown route %s".formatted(path));
        }

        // Check: HTTP_METHOD + ROUTE is a valid combination
        var functionType = supportedAccesses.get(httpMethod + path);

        if (functionType == null) {
            throw new IllegalStateException("Route %s does not support HTTP method %s".formatted(path, httpMethod));
        }

        var end = System.nanoTime();
        Measurements.add("Resolve & check call `%s`".formatted(functionCtx.functionName.getText()), end - start);
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
