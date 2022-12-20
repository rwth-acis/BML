package i5.bml.parser.types.openapi;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;

@BMLType(name = BuiltinType.OPENAPI_SCHEMA, isComplex = true)
public class BMLOpenAPISchema extends AbstractBMLType {

    // TODO: Track required attributes/properties/fields

    private String schemaName;

    public BMLOpenAPISchema() {}

    public BMLOpenAPISchema(String schemaName, Map<String, Type> supportedAccesses) {
        this.schemaName = schemaName;
        this.supportedAccesses = supportedAccesses;
    }

    @Override
    public String getName() {
        return schemaName;
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return ctx.getText().equals("code") ? TypeRegistry.resolveType(BuiltinType.NUMBER) : supportedAccesses.get(ctx.getText());
    }
}
