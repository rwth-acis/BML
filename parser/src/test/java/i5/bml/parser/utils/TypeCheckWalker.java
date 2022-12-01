package i5.bml.parser.utils;

import generatedParser.BMLParser;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Scope;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.function.BiConsumer;

public class TypeCheckWalker extends DiagnosticsCollector {

    private final BiConsumer<Scope, ParserRuleContext> verifier;

    public TypeCheckWalker(BiConsumer<Scope, ParserRuleContext> verifier) {
        this.verifier = verifier;
    }

    @Override
    public void exitAssignment(BMLParser.AssignmentContext ctx) {
        super.exitAssignment(ctx);
        verifier.accept(currentScope, ctx);
    }
}
