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
    public void enterFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        FunctionSymbol f = new FunctionSymbol(ctx.head.functionName.getText());
        f.setEnclosingScope(currentScope);
        ctx.scope = f;
        pushScope(f);
    }

    @Override
    public void enterFunctionHead(BMLParser.FunctionHeadContext ctx) {
        currentScope.define(new VariableSymbol(ctx.parameterName.getText()));
    }

    protected void pushScope(Scope s) {
        currentScope = s;
        System.out.println("entering: " + currentScope.getName() + ":" + s.getSymbols());
    }

    protected void popScope() {
        System.out.println("leaving: " + currentScope.getName() + ":" + currentScope.getSymbols());
        currentScope = currentScope.getEnclosingScope();
    }

    // TODO: Check whether symbols have already been defined, same goes for elementList, etc.

    public Scope getCurrentScope() {
        return currentScope;
    }
}
