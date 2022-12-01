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
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static i5.bml.parser.errors.ParserError.*;

@BMLType(name = BuiltinType.OPENAPI, isComplex = true)
public class BMLOpenAPIComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

    private OpenAPI openAPI;

    private Set<String> routes;

    private final Set<String> httpMethods = new HashSet<>();

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        if (ctx == null) { // Missing parameters, but it has been reported by `checkParameters`
            return;
        }

        var expr = ctx.elementExpressionPair(0).expr;
        var atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    EXPECTED_BUT_FOUND.format(BuiltinType.STRING, expr.type), expr);
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
            super.addDiagnostic("Could not connect to url `%s`".formatted(url), DiagnosticSeverity.Error);
            return;
        }
        // Check for OpenAPI parser errors
        else if ((result.getMessages() != null && result.getMessages().size() > 0)) {
            super.addDiagnostic("Could not connect to url `%s`\nPossible reason(s):\n%s"
                    .formatted(url, String.join("\n", result.getMessages())), DiagnosticSeverity.Error);
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

            var p = new ParameterSymbol("path");
            p.setType(TypeRegistry.resolveType(BuiltinType.STRING));
            requiredParameters.add(p);

            var function = new BMLFunction(returnType, requiredParameters, optionalParameters);
            supportedAccesses.put(httpMethod.name().toLowerCase() + route, function);
            httpMethods.add(httpMethod.name().toLowerCase());
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
                    super.addDiagnostic(msg.formatted(httpMethod.toUpperCase(), route, responseCode), DiagnosticSeverity.Warning);
                } else {
                    var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(mediaType.getSchema(),
                            "Operation", operation.getOperationId());
                    returnType[0] = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
                }
            }
        });

        return returnType[0];
    }

    private void computeArgumentTypes(List<ParameterSymbol> arguments, Schema<?> schema, String parameterName) {
        if (schema == null) {
            var msg = "Couldn't find schema for parameter `%s`".formatted(parameterName);
            super.addDiagnostic(msg, DiagnosticSeverity.Warning);
            return;
        }

        var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(schema, "Parameter", parameterName);
        var resolvedBMLType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);

        var parameterSymbol = new ParameterSymbol(parameterName);
        parameterSymbol.setType(resolvedBMLType);
        arguments.add(parameterSymbol);
    }

    private Pair<ArrayList<ParameterSymbol>, ArrayList<ParameterSymbol>> computeRouteArguments(String route,
                                                                                               String httpMethod,
                                                                                               Operation operation) {
        var requiredArguments = new ArrayList<ParameterSymbol>();
        var optionalArguments = new ArrayList<ParameterSymbol>();

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
                super.addDiagnostic(msg.formatted(httpMethod.toUpperCase(), route),
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
            Diagnostics.addDiagnostic(diagnostics, NOT_DEFINED.format(httpMethod), functionCallCtx.functionName);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: path is specified
        var pathParameter = functionCallCtx.elementExpressionPairList().elementExpressionPair().stream()
                .filter(p -> p.name.getText().equals("path"))
                .findAny();

        if (pathParameter.isEmpty()) {
            Diagnostics.addDiagnostic(diagnostics, MISSING_PARAM.format("path"), functionCallCtx);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: path parameter has correct type
        var pathParameterType = pathParameter.get().expression().type;
        if (!pathParameterType.equals(TypeRegistry.resolveType(BuiltinType.STRING))) {
            Diagnostics.addDiagnostic(diagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.STRING, pathParameterType),
                    pathParameter.get().expression());
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: route is valid
        var path = pathParameter.get().expression().getText();
        path = path.substring(1, path.length() - 1);

        if (!routes.contains(path)) {
            Diagnostics.addDiagnostic(diagnostics, "Path `%s` is not defined for API:\n`%s`"
                    .formatted(path, url), pathParameter.get().expression());
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        // Check: HTTP_METHOD + ROUTE is a valid combination
        var functionType = supportedAccesses.get(httpMethod + path);

        if (functionType == null) {
            Diagnostics.addDiagnostic(diagnostics, "Path `%s` does not support HTTP method `%s` for API:\n`%s`"
                            .formatted(path, httpMethod, url), pathParameter.get().expression());
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        var end = System.nanoTime();
        Measurements.add("Resolve & check call `%s`".formatted(functionCallCtx.functionName.getText()), end - start);
        return functionType;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String encodeToString() {
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
