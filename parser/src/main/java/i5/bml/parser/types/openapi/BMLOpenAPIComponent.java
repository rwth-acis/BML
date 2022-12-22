package i5.bml.parser.types.openapi;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.*;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.*;

import static i5.bml.parser.errors.ParserError.*;

@BMLType(name = BuiltinType.OPENAPI, isComplex = true)
public class BMLOpenAPIComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

    private OpenAPI openAPI;

    private Set<String> routes;

    private final Set<String> httpMethods = new HashSet<>();

    /**
     * `HTTP method` + `route` -> Pair(Tag, OperationId)
     */
    private final Map<String, Pair<String, String>> tagOperationIdPairs = new HashMap<>();

    @Override
    public void checkParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        if (ctx == null) {
            return;
        }

        var parameterListMutable = new HashSet<>(ctx.elementExpressionPair());

        for (var requiredParameter : requiredParameters) {
            var name = requiredParameter.getName();

            var invocationParameter = parameterListMutable.stream()
                    .filter(p -> p.name.getText().equals(name))
                    .findAny();

            if (invocationParameter.isEmpty()) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(), MISSING_PARAM.format(name), ctx);
            } else {
                requiredParameter.setExprCtx(invocationParameter.get().expr);
                var invocationParameterType = invocationParameter.get().expr.type;
                if (requiredParameter.getAllowedTypes().stream().noneMatch(t -> t.equals(invocationParameterType))) {
                    if (invocationParameterType instanceof BMLOpenAPISchema schema) {

                    } else {
                        var errorMessage = new StringBuilder();
                        errorMessage.append("Expected any of ");
                        for (Type allowedType : requiredParameter.getAllowedTypes()) {
                            errorMessage.append("´").append(allowedType).append("´, ");
                        }
                        var i = errorMessage.lastIndexOf(", ");
                        errorMessage.delete(i, i + 2);
                        errorMessage.append(" but found ´").append(invocationParameterType).append("`");
                        Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(), errorMessage.toString(),
                                invocationParameter.get());
                    }
                }

                parameterListMutable.remove(invocationParameter.get());
            }
        }

        super.checkOptionalParameters(diagnosticsCollector, parameterListMutable);
    }

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        var expr = ctx.elementExpressionPair(0).expr;
        var atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format("url", BuiltinType.STRING), expr);
        } else {
            url = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }

    @Override
    public void initializeType(ParserRuleContext ctx) {
        if (url == null) { // Missing URL parameter, but it has been reported by `checkParameters`
            return;
        }

        var start = System.nanoTime();
        SwaggerParseResult result = new OpenAPIParser().readLocation(url, null, null);
        var end = System.nanoTime();
        Measurements.add("Fetch OpenAPI Spec", (end - start));

        openAPI = result.getOpenAPI();

        if (openAPI == null) {
            super.cacheDiagnostic(CONNECT_FAILED.format(url), DiagnosticSeverity.Error);
            return;
        }
        // Check for OpenAPI parser errors
        else if ((result.getMessages() != null && result.getMessages().size() > 0)) {
            super.cacheDiagnostic("%s\nPossible reason(s):\n%s"
                    .formatted(CONNECT_FAILED.format(url), String.join("\n", result.getMessages())),
                    DiagnosticSeverity.Error);
            return;
        }

        // Set valid OpenAPI routes
        routes = openAPI.getPaths().keySet();

        // Determine route return types & arguments
        start = System.nanoTime();
        openAPI.getPaths().forEach((route, value) -> value.readOperationsMap().forEach((httpMethod, operation) -> {
            AbstractBMLType returnType = (AbstractBMLType) computeRouteReturnTypes(route, httpMethod.name(), operation);

            var routeParameters = computeRouteArguments(route, httpMethod.name(), operation);
            var requiredParameters = routeParameters.getLeft();
            var optionalParameters = routeParameters.getRight();

            var p = new BMLFunctionParameter("path");
            p.setType(TypeRegistry.resolveType(BuiltinType.STRING));
            requiredParameters.add(p);

            var function = new BMLFunctionType(returnType, requiredParameters, optionalParameters);
            supportedAccesses.put(httpMethod.name().toLowerCase() + route, function);
            httpMethods.add(httpMethod.name().toLowerCase());

            // We store a map with `HTTP method + route` -> `Tag` for code generation
            if (operation.getTags() == null || operation.getTags().isEmpty()) {
                tagOperationIdPairs.put(httpMethod.name().toLowerCase() + route.toLowerCase(),
                        new ImmutablePair<>("Default", operation.getOperationId()));
            } else {
                tagOperationIdPairs.put(httpMethod.name().toLowerCase()  + route.toLowerCase(),
                        new ImmutablePair<>(operation.getTags().get(0), operation.getOperationId()));
            }
        }));
        end = System.nanoTime();
        Measurements.add("Parsing OpenAPI Spec", end - start);
    }

    private Type computeRouteReturnTypes(String route, String httpMethod, Operation operation) {
        final Type[] returnType = new Type[]{null};
        operation.getResponses().forEach((responseCode, response) -> {
            if (response.getContent() != null) {
                // TODO: ATM we only support application/json
                var mediaType = response.getContent().get("application/json");
                if (mediaType == null) {
                    var msg = "`%s %s` has no application/json media type for response code `%s`\n";
                    super.cacheDiagnostic(msg.formatted(httpMethod.toUpperCase(), route, responseCode), DiagnosticSeverity.Warning);
                } else {
                    var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(mediaType.getSchema(),
                            "Operation", operation.getOperationId());
                    returnType[0] = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
                }
            }
        });

        // TODO: We need a void type for, e.g., POST /pet/{petId}
        return returnType[0];
    }

    private void computeArgumentTypes(List<BMLFunctionParameter> arguments, Schema<?> schema, String parameterName) {
        if (schema == null) {
            var msg = "Couldn't find schema for parameter `%s`".formatted(parameterName);
            super.cacheDiagnostic(msg, DiagnosticSeverity.Warning);
            return;
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);

        var parameter = new BMLFunctionParameter(parameterName);
        parameter.setType(resolvedBMLType);
        arguments.add(parameter);
    }

    private Pair<ArrayList<BMLFunctionParameter>, ArrayList<BMLFunctionParameter>> computeRouteArguments(String route,
                                                                                               String httpMethod,
                                                                                               Operation operation) {
        var requiredArguments = new ArrayList<BMLFunctionParameter>();
        var optionalArguments = new ArrayList<BMLFunctionParameter>();

        // Parameters
        if (operation.getParameters() != null && operation.getParameters().size() > 0) {
            // Required parameters
            operation.getParameters().stream()
                    .filter(Parameter::getRequired)
                    .forEach(p -> computeArgumentTypes(requiredArguments, p.getSchema(), p.getName()));

            // Optional parameters
            operation.getParameters().stream()
                    .filter(p -> !p.getRequired())
                    .forEach(p -> computeArgumentTypes(optionalArguments, p.getSchema(), p.getName()));
        }

        // Request body
        if (operation.getRequestBody() != null) {
            // TODO: ATM we only support application/json
            var mediaType = operation.getRequestBody().getContent().get("application/json");
            if (mediaType == null) {
                var msg = "`%s %s` has no application/json media type";
                super.cacheDiagnostic(msg.formatted(httpMethod.toUpperCase(), route),
                        DiagnosticSeverity.Warning);
            } else {
                var required = operation.getRequestBody().getRequired();
                if (required == null || Boolean.FALSE.equals(required)) {
                    computeArgumentTypes(optionalArguments, mediaType.getSchema(), "body");
                } else {
                    computeArgumentTypes(requiredArguments, mediaType.getSchema(), "body");
                }
            }
        }

        return new ImmutablePair<>(requiredArguments, optionalArguments);
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        var start = System.nanoTime();
        var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
        var httpMethod = functionCallCtx.functionName.getText();
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        // Check: http method is valid
        if (!httpMethods.contains(httpMethod)) {
            // TODO: The error message could be improved (e.g., more context)
            Diagnostics.addDiagnostic(diagnostics, NOT_DEFINED.format(httpMethod), functionCallCtx.functionName);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: route is valid
        var pathParameter = functionCallCtx.elementExpressionPairList().elementExpressionPair().stream()
                .filter(p -> p.name.getText().equals("path"))
                .findAny();
        var path = pathParameter.get().expression().getText();
        path = path.substring(1, path.length() - 1);

        if (!routes.contains(path)) {
            Diagnostics.addDiagnostic(diagnostics, NO_PATH_FOR_API.format(path, url), pathParameter.get().expression());
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: HTTP_METHOD + ROUTE is a valid combination
        var functionType = supportedAccesses.get(httpMethod + path);

        if (functionType == null) {
            Diagnostics.addDiagnostic(diagnostics, METHOD_NOT_SUPPORTED.format(path, httpMethod, url),
                    pathParameter.get().expression());
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        var end = System.nanoTime();
        Measurements.add("Resolve & check call `%s`".formatted(functionCallCtx.functionName.getText()), end - start);
        return functionType;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Pair<String, String>> getTagOperationIdPairs() {
        return tagOperationIdPairs;
    }

    @Override
    public String encodeToString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }
}
