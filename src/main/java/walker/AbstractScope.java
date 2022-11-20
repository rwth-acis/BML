package walker;

import generatedParser.BMLBaseListener;
import org.antlr.symtab.Scope;

public abstract class AbstractScope extends BMLBaseListener {
    protected Scope currentScope;

    protected void pushScope(Scope s) {
        currentScope = s;
        System.out.println("entering: " + currentScope.getName() + ":" + s.getSymbols());
    }

    protected void popScope() {
        System.out.println("leaving: " + currentScope.getName() + ":" + currentScope.getSymbols());
        currentScope = currentScope.getEnclosingScope();
    }
}
