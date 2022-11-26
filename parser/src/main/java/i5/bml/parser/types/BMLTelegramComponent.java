package i5.bml.parser.types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

@BMLType(index=1, typeString = "Telegram")
public class BMLTelegramComponent extends AbstractBMLType {

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }
}
