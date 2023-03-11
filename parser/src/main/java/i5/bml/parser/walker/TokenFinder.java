package i5.bml.parser.walker;

import generatedParser.BMLBaseListener;
import org.antlr.symtab.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;

public class TokenFinder extends BMLBaseListener {

    public static Pair<ParseTree, Scope> findToken(ParseTree root, int row, int column) {
        Queue<ParseTree> toVisit = new ArrayDeque<>();
        toVisit.add(root);
        ParseTree curr;
        Scope currScope = null;
        while ((curr = toVisit.poll()) != null) {
            if (curr instanceof TerminalNode terminalNode) {
                var token = terminalNode.getSymbol();
                if (token.getLine() != row) {
                    continue;
                }

                var tokenStop = token.getCharPositionInLine() + (token.getStopIndex() - token.getStartIndex() + 1);
                if (token.getCharPositionInLine() <= column && tokenStop >= column) {
                    return new ImmutablePair<>(terminalNode, currScope);
                }
            } else {
                var ctx = (ParserRuleContext) curr;

                if (ctx.start != null && ctx.stop != null
                        && ctx.start.getLine() <= row && (ctx.start.getLine() != row || column >= ctx.start.getCharPositionInLine())) {
                    var tokenStop = ctx.stop.getCharPositionInLine() + (ctx.stop.getStopIndex() - ctx.stop.getStartIndex() + 1);
                    if (ctx.stop.getLine() < row || (ctx.stop.getLine() == row && tokenStop < column)) {
                        continue;
                    }

                    var scopeField = Arrays.stream(curr.getClass().getDeclaredFields()).filter(f -> f.getName().equals("scope")).findAny();
                    if (scopeField.isPresent()) {
                        try {
                            var canAccess = scopeField.get().canAccess(curr);
                            scopeField.get().setAccessible(true);
                            currScope = Objects.requireNonNullElse((Scope) scopeField.get().get(curr), currScope);
                            scopeField.get().setAccessible(canAccess);
                        } catch (IllegalAccessException ignore) {}
                    }

                    toVisit.addAll(ctx.children);
                }
            }
        }

        return null;
    }
}
