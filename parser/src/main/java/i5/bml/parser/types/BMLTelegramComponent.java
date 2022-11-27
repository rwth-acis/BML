package i5.bml.parser.types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

@BMLType(name = "Telegram", isComplex = true)
public class BMLTelegramComponent extends AbstractBMLType {

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }
}
