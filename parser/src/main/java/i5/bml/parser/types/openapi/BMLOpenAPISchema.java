package i5.bml.parser.types.openapi;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;

import java.util.Map;

@BMLType(name = "OpenAPISchema", isComplex = true)
public class BMLOpenAPISchema extends AbstractBMLType {

    // TODO: Track required attributes/properties/fields

    private String schemaName;

    public BMLOpenAPISchema() {}

    public BMLOpenAPISchema(String schemaName) {
        this.schemaName = schemaName;
    }

    public BMLOpenAPISchema(String schemaName, Map<String, Type> supportedAccesses) {
        this.schemaName = schemaName;
        this.supportedAccesses = supportedAccesses;
    }

    @Override
    public String getName() {
        return super.getName() + "<" + schemaName + ">";
    }

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return supportedAccesses.get(ctx.getText());
    }

    @Override
    public Object clone() {
        return new BMLOpenAPISchema(schemaName);
    }
}
