package walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.antlr.symtab.*;

public class SymbolTableAndScopeGenerator extends BMLBaseListener {

    private Scope currentScope;

    @Override
    public void enterBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        GlobalScope g = new GlobalScope(null);
        ctx.scope = g;
        pushScope(g);
    }

    @Override
    public void exitBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        popScope();
    }

    @Override
    public void enterComponent(BMLParser.ComponentContext ctx) {
        currentScope.define(new VariableSymbol(ctx.name.getText()));
    }

    @Override
    public void enterEventListenerDeclaration(BMLParser.EventListenerDeclarationContext ctx) {
        FunctionSymbol f = new FunctionSymbol(ctx.head.listenerName.getText());
        f.setEnclosingScope(currentScope);
        ctx.scope = f;
        pushScope(f);
    }

    protected void pushScope(Scope s) {
        currentScope = s;
        System.out.println("entering: " + currentScope.getName() + ":" + s.getSymbols());
    }

    protected void popScope() {
        System.out.println("leaving: " + currentScope.getName() + ":" + currentScope.getSymbols());
        currentScope = currentScope.getEnclosingScope();
    }

    public Scope getCurrentScope() {
        return currentScope;
    }
}
