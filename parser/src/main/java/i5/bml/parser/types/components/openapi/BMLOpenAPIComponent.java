package i5.bml.parser.types.components.openapi;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.types.*;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static i5.bml.parser.errors.ParserError.*;

@BMLType(name = BuiltinType.OPENAPI, isComplex = true)
public class BMLOpenAPIComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

    private OpenAPI openAPI;

    private String openAPISpec;

    private String apiName;

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
                    // TODO: Convert to map object
                    addTypeErrorMessage(diagnosticsCollector, invocationParameter.get(), requiredParameter);
                }

                parameterListMutable.remove(invocationParameter.get());
            }
        }

        super.checkOptionalParameters(diagnosticsCollector, parameterListMutable);
    }

    @Override
    public void initializeType(ParserRuleContext ctx) {
        if (url == null) { // Missing URL parameter, but it has been reported by `checkParameters`
            return;
        }

        var success = Measurements.measure("Fetching OpenAPI spec", this::getOpenAPISpec);
        if (Boolean.FALSE.equals(success)) {
            return;
        }

        var result = Measurements.measure("OpenAPI spec parser", () -> new OpenAPIParser().readContents(openAPISpec, null, null));

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

        Measurements.measure("Generating types from OpenAPI spec", this::parseOpenAPISpec);
    }

    private boolean getOpenAPISpec() {
        try (var s = new Scanner(new URL(url).openStream(), StandardCharsets.UTF_8)) {
            openAPISpec = s.useDelimiter("\\A").next();
            return true;
        } catch (Exception e) {
            super.cacheDiagnostic(CONNECT_FAILED.format(url), DiagnosticSeverity.Error);
            return false;
        }
    }

    private void parseOpenAPISpec() {
        // Determine route return types & arguments
        openAPI.getPaths().forEach((route, value) -> value.readOperationsMap().forEach((httpMethod, operation) -> {
            AbstractBMLType returnType = (AbstractBMLType) computeRouteReturnTypes(route, httpMethod.name(), operation);
            if (returnType != null) {
                returnType.getSupportedAccesses().put("code", TypeRegistry.resolveType(BuiltinType.NUMBER));
            }

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
    }

    private Type computeRouteReturnTypes(String route, String httpMethod, Operation operation) {
        var entry = operation.getResponses().entrySet().stream()
                .filter(e -> e.getValue().getContent() != null)
                .findAny();

        if (entry.isPresent()) {
            // TODO: ATM we only support application/json
            var mediaType = entry.get().getValue().getContent().get("application/json");
            if (mediaType == null) {
                var msg = "`%s %s` has no application/json media type for response code `%s`\n";
                super.cacheDiagnostic(msg.formatted(httpMethod.toUpperCase(), route, entry.get().getKey()), DiagnosticSeverity.Warning);
                return null;
            } else {
                var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(mediaType.getSchema(),
                        "Operation", operation.getOperationId());
                return BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(this, openAPITypeToResolve);
            }
        } else {
            var resolvedOpenAPIType = TypeRegistry.resolveType("empty");
            if (resolvedOpenAPIType == null) {
                Map<String, Type> supportedFields = new HashMap<>();
                supportedFields.put("code", TypeRegistry.resolveType(BuiltinType.NUMBER));

                // Add to type registry
                var newType = new BMLOpenAPISchema(this, "empty", supportedFields);
                TypeRegistry.registerType(newType);
                return newType;
            } else {
                return resolvedOpenAPIType;
            }
        }
    }

    private void computeArgumentTypes(List<BMLFunctionParameter> arguments, Schema<?> schema, String parameterName) {
        if (schema == null) {
            var msg = "Couldn't find schema for parameter `%s`".formatted(parameterName);
            super.cacheDiagnostic(msg, DiagnosticSeverity.Warning);
            return;
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(this, openAPITypeToResolve);

        var parameter = new BMLFunctionParameter(parameterName, resolvedBMLType);
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

        // Not covered by normal parameter checks since we cannot invoke them without a function type
        if (pathParameter.isEmpty()) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(), MISSING_PARAM.format("path"), functionCallCtx);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        } else if (!pathParameter.get().expression().type.equals(TypeRegistry.resolveType(BuiltinType.STRING))) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    EXPECTED_BUT_FOUND.format(BuiltinType.STRING, BuiltinType.NUMBER), functionCallCtx);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        var path = pathParameter.get().expression().getText();
        if (path.startsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }

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

        return new BMLFunctionType((BMLFunctionType) functionType);
    }

    public OpenAPI openAPI() {
        return openAPI;
    }

    public String openAPISpec() {
        return openAPISpec;
    }

    public String url() {
        return url;
    }

    public Map<String, Pair<String, String>> tagOperationIdPairs() {
        return tagOperationIdPairs;
    }

    @Override
    public String encodeToString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }

    public String apiName() {
        return apiName;
    }

    public void apiName(String apiName) {
        this.apiName = apiName;
    }
}
