package types.openapi;

import types.AbstractBMLType;
import types.BMLType;

@BMLType(index = 7, typeString = "OpenAPISchema")
public class BMLOpenAPISchema extends AbstractBMLType {

    private String schemaName;

    public BMLOpenAPISchema() {}

    public BMLOpenAPISchema(String schemaName) {
        this.schemaName = schemaName;
    }

    @Override
    public String getName() {
        return super.getName() + "<" + schemaName + ">";
    }

    @Override
    public Object clone() {
        return new BMLOpenAPISchema(schemaName);
    }
}
