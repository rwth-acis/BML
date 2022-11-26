package types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

@BMLType(index = 5, typeString = "String")
public class BMLString extends AbstractBMLType {

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }
}
