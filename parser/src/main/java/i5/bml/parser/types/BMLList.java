package i5.bml.parser.types;

import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

@BMLType(name = BuiltinType.LIST, isComplex = true)
public class BMLList extends AbstractBMLType implements Summable {

    private Type itemType;

    public BMLList() {}

    public BMLList(Type itemType) {
        this.itemType = itemType;
    }

    @Override
    public String getName() {
        return "%s<%s>".formatted(super.getName(), itemType.getName());
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return ((AbstractBMLType) itemType).resolveAccess(diagnosticsCollector, ctx);
    }

    public Type getItemType() {
        return itemType;
    }

    @Override
    public String encodeToString() {
        return "%s{itemType=%s}".formatted(getName(), itemType);
    }
}
