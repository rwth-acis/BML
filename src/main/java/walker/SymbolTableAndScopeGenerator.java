package walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.antlr.symtab.*;
import types.TypeRegistry;

public class SymbolTableAndScopeGenerator extends BMLBaseListener {

    protected Scope currentScope;

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

    @Override
    public void exitEventListenerDeclaration(BMLParser.EventListenerDeclarationContext ctx) {
        popScope();
    }

    private void pushScope(Scope s) {
        currentScope = s;
        System.out.println("entering: " + currentScope.getName() + ":" + s.getSymbols());
    }

    private void popScope() {
        System.out.println("leaving: " + currentScope.getName() + ":" + currentScope.getSymbols());
        currentScope = currentScope.getEnclosingScope();
    }
}
