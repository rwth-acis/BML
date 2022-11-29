package i5.bml.parser.types;

import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

@BMLType(name = "List", isComplex = true)
public class BMLList extends AbstractBMLType {

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
    public String toString() {
        return "%s{itemType=%s}".formatted(getName(), itemType);
    }
}
