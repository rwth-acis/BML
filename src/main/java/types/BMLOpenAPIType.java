package types;

import io.swagger.v3.oas.models.OpenAPI;

@BMLType(index = 4, typeString = "OpenAPI")
public class BMLOpenAPIType extends AbstractBMLType {

    private String url;

    private OpenAPI openAPI;
}
