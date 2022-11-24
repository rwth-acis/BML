package types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

@BMLType(index = 6, typeString = "List")
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
    public Type resolveAccess(ParseTree ctx) {
        return ((AbstractBMLType) itemType).resolveAccess(ctx);
    }
}
