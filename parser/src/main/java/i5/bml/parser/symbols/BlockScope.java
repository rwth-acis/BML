package i5.bml.parser.symbols;

import org.antlr.symtab.BaseScope;
import org.antlr.symtab.Scope;

/**
 * Represents a block scope in a BML program.
 * Also see {@link BaseScope}.
 */
public class BlockScope extends BaseScope {

    /**
     * Creates a new instance of block scope with given enclosing scope.
     *
     * @param enclosingScope The enclosing scope of this block scope.
     */
    public BlockScope(Scope enclosingScope) {
        super(enclosingScope);
    }

    /**
     * Returns the name of this scope, which is "local".
     *
     * @return the name of the scope.
     */
    @Override
    public String getName() {
        return "local";
    }
}

