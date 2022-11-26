package i5.bml.parser.types;

import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

@BMLType(index = 2, typeString = "Boolean")
public class BMLBoolean extends AbstractBMLType {

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }
}