package types;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.*;

@BMLType(index = 4, typeString = "OpenAPI")
public class BMLOpenAPIComponent extends AbstractBMLType {

    private String url;

    private OpenAPI openAPI;

    private Map<String, ArrayList<String>> openAPITypes = new HashMap<>();

    private Map<String, String> routeReturnTypes = new HashMap<>();

    /**
     * HTTP_METHOD + ROUTE -> Set(Parameters)
     */
    private Map<String, Set<BMLOpenAPIParameter>> routeParameters = new HashMap<>();

//    @BMLFunction
//    public BMLOpenAPIResponse get()

    @InitializerMethod
    public void retrieveOpenAPISchema() {
        Objects.requireNonNull(url);

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation(url, null, parseOptions);
        openAPI = result.getOpenAPI();

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

        for (String s : openAPI.getComponents().getSchemas().keySet()) {

        }

        // Determine route return types
        openAPI.getPaths().forEach((route, value) -> {

        });

//        System.out.println(((Schema) openAPI.getComponents().getSchemas().get("Pet").getProperties().get("id")).getType());
//        Schema<?> schema = openAPI.getPaths().get("/pet/findByTags").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
//        System.out.println(schema.getType());
//        System.out.println(schema.getItems());
    }
}