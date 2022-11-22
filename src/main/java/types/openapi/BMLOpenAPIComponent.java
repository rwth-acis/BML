package types.openapi;

import generatedParser.BMLParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.antlr.symtab.Type;
import types.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@BMLType(index = 4, typeString = "OpenAPI")
public class BMLOpenAPIComponent extends AbstractBMLType {

    private String url;

    private Set<String> routes;

    /**
     * HTTP_METHOD + ROUTE -> OpenAPIResponse Object (pre-configured, ready for cloning)
     */
    private final Map<String, BMLOpenAPIResponse> routeReturnTypes = new HashMap<>();

    /**
     * HTTP_METHOD + ROUTE -> Set(Parameters)
     */
    private final Map<String, Set<BMLOpenAPIRequestParameter>> routeRequiredParameters = new HashMap<>();

    private final Map<String, Set<BMLOpenAPIRequestParameter>> routeOptionalParameters = new HashMap<>();

    private final Queue<Object> bmlCheckData = new ArrayDeque<>();

    @InitializerMethod
    public void retrieveOpenAPISchema() {
        Objects.requireNonNull(url);

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation(url, null, parseOptions);
        OpenAPI openAPI = result.getOpenAPI();

        // Check for errors
        if (result.getMessages() != null && result.getMessages().size() > 0) {
            StringBuilder s = new StringBuilder();
            s.append("Encountered error messages for url '%s':\n".formatted(url));
            result.getMessages().forEach(m -> {
                s.append(m).append("\n");
            });

            // TODO: Proper error handling
            throw new IllegalStateException(s.toString());
        }

        if (openAPI == null) {
            throw new IllegalStateException("Could not connect to url '%s'".formatted(url));
        }

        // Set valid OpenAPI component types
        routes = openAPI.getPaths().keySet();

        // Set valid OpenAPI routes


        // Determine route return types & parameters
        openAPI.getPaths().forEach((route, value) -> {
            value.readOperationsMap().forEach((httpMethod, operation) -> {
                // Route return types
                var responseType = computeRouteReturnTypes(operation);
                routeReturnTypes.put(httpMethod.name().toLowerCase() + route, responseType);

                // Required route parameters & request bodies
                var parameterSet = computeRouteParameters(operation, true);
                routeRequiredParameters.put(httpMethod.name().toLowerCase() + route, parameterSet);

                parameterSet = computeRouteParameters(operation, false);
                routeOptionalParameters.put(httpMethod.name().toLowerCase() + route, parameterSet);
            });
        });
    }

    private BMLOpenAPIResponse computeRouteReturnTypes(Operation operation) {
        if (operation.getResponses() == null) {
            // TODO
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
                    var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPITypeToResolve);
                    return new BMLOpenAPIResponse(resolvedBMLType);
                }
            }
        }

        //throw new IllegalStateException("Could not compute response type of operation %s".formatted(operation.getOperationId()));
        return null; // TODO: We need a void type
    }

    private void computeParameterTypes(Set<BMLOpenAPIRequestParameter> parameterSet, Schema<?> schema, String parameterName) {
        if (schema == null) {
            // TODO
            throw new IllegalStateException("Couldn't find schema for parameter %s"
                    .formatted(parameterName));
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPITypeToResolve);

        parameterSet.add(new BMLOpenAPIRequestParameter(parameterName, resolvedBMLType));
    }

    private Set<BMLOpenAPIRequestParameter> computeRouteParameters(Operation operation, boolean collectRequired) {
        var parameterSet = new HashSet<BMLOpenAPIRequestParameter>();
        if (operation.getParameters() != null && operation.getParameters().size() > 0) {
            operation.getParameters().stream()
                    .filter(p -> p.getRequired() == collectRequired)
                    .forEach(p -> computeParameterTypes(parameterSet, p.getSchema(), p.getName()));
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
                // TODO: for now we ignore other media types than application/json
                System.err.printf("Operation `%s` has no application/json media type\n", operation.getOperationId());
            } else {
                var r = operation.getRequestBody().getRequired();
                if ((r == null && !collectRequired) || (r != null && r == collectRequired)) {
                    computeParameterTypes(parameterSet, mediaType.getSchema(), "body");
                }
            }
        }

        return parameterSet;
    }


    @BMLCheck(index = 1)
    public void routeIsValid(BMLParser.FunctionInvocationContext ctx) {
        var parameterList = ctx.elementExpressionPairList().elementExpressionPair();
        var pathParameter = parameterList.stream()
                .filter(p -> p.name.getText().equals("path"))
                .findAny();

        // We can assume that path is present because of checks done in the TypeSynthesizer
        //noinspection OptionalGetWithoutIsPresent
        var path = pathParameter.get().expression().getText();
        path = path.substring(1, path.length() - 1);

        if (!routes.contains(path)) {
            throw new IllegalStateException("Route %s is not specified".formatted(path));
        }

        // Pass data to next check
        bmlCheckData.add(path);
    }

    @BMLCheck(index = 2)
    public void checkRequiredParameters(BMLParser.FunctionInvocationContext ctx) {
        // We can assume that the route exists, we need to make sure that the HTTP method for the route exists
        var parameterListMutable = new ArrayList<>(ctx.elementExpressionPairList().elementExpressionPair());
        var path = (String) bmlCheckData.peek();
        var requiredParameters = routeRequiredParameters.get(ctx.functionName.getText() + path);

        // Check: HTTP method exists for specified route
        if (requiredParameters == null) {
            throw new IllegalStateException("HTTP method %s is not defined for route %s"
                    .formatted(ctx.functionName.getText(), path));
        }

        for (var requiredParameter : requiredParameters) {
            // Name
            var name = requiredParameter.name();

            var invocationParameter = parameterListMutable
                    .stream()
                    .filter(p -> p.name.getText().equals(name))
                    .findAny();

            if (invocationParameter.isEmpty()) {
                throw new IllegalStateException("Parameter %s is required but not present for function call %s"
                        .formatted(name, ctx.getText()));
            }

            // Type
            var requiredParameterType = requiredParameter.type();
            var invocationParameterType = invocationParameter.get().expression().type;
            if (!requiredParameterType.equals(invocationParameterType)) {
                throw new IllegalStateException("Parameter %s requires type %s but found type %s"
                        .formatted(name, requiredParameterType.getName(),
                                invocationParameterType.getName()));
            }

            parameterListMutable.remove(invocationParameter.get());
        }

        // TODO: This is not very nicely done
        // We remove the path parameter since it is not part of the OpenAPI spec
        var remainingParameters = parameterListMutable.stream()
                .filter(p -> !p.name.getText().equals("path"))
                .toList();
        bmlCheckData.add(remainingParameters);

        // Synthesize BMLOpenAPIResponse with OpenAPI component type, etc.
    }

    @BMLCheck(index = 3)
    public void checkOptionalParameters(BMLParser.FunctionInvocationContext ctx) {
        var path = (String) bmlCheckData.remove();
        @SuppressWarnings("unchecked")
        var remainingParameterList = (List<BMLParser.ElementExpressionPairContext>) bmlCheckData.remove();
        // We can assume that key is present, since it was checked in `checkRequiredParameters`
        var optionalParameters = routeOptionalParameters.get(ctx.functionName.getText() + path);

        for (var parameterPair : remainingParameterList) {
            // Name
            var name = parameterPair.name.getText();
            var optionalParameter = optionalParameters.stream()
                    .filter(p -> p.name().equals(name))
                    .findAny();

            if (optionalParameter.isEmpty()) {
                // TODO
                throw new IllegalStateException("Parameter %s is not defined for HTTP method %s with route %s"
                        .formatted(name, ctx.functionName.getText(), path));
            }

            // We can assume that parameter is present, so we expect the correct type
            var optionalParameterType = optionalParameter.get().type();
            var invocationParameterType = parameterPair.expression().type;
            if (!optionalParameterType.equals(invocationParameterType)) {
                throw new IllegalStateException("Parameter %s requires type %s but found type %s"
                        .formatted(name, optionalParameterType.getName(),
                                invocationParameterType.getName()));
            }
        }
    }

    

    @BMLFunction
    public void get(@BMLFunctionParameter(name = "path") BMLString p) {
    }

    @BMLFunction
    public void put(@BMLFunctionParameter(name = "path") BMLString p) {
    }

    @BMLFunction
    public void post(@BMLFunctionParameter(name = "path") BMLString p) {
    }

    public Set<String> getRoutes() {
        return routes;
    }
}
