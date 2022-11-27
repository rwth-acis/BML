package i5.bml.parser.symbols;

import org.antlr.symtab.BaseScope;
import org.antlr.symtab.Scope;

public class BlockScope extends BaseScope {

    public BlockScope(Scope enclosingScope) {
        super(enclosingScope);
    }

    @Override
    public String getName() {
        return "local";
    }
}
