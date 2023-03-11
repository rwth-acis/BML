package i5.bml.parser;

import generatedParser.BMLLexer;
import generatedParser.BMLParser;
import i5.bml.parser.errors.SyntaxErrorListener;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.parser.walker.TerminalNodeFinder;
import org.antlr.symtab.Scope;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Diagnostic;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Parser {

    private Parser() {}

    public static Pair<ParseTree, Scope> findTerminalNode(ParseTree parseTree, int row, int column) {
        return TerminalNodeFinder.findTerminalNode(parseTree, row, column);
    }

    public static Pair<ParseTree, List<Diagnostic>> parseAndCollectDiagnostics(String inputString, StringBuilder report) {
        var bmlParser = bmlParser(inputString);

        var syntaxErrorListener = new SyntaxErrorListener();
        bmlParser.removeErrorListeners();
        bmlParser.addErrorListener(syntaxErrorListener);

        var diagnosticsCollector = new DiagnosticsCollector();
        var tree = bmlParser.program();
        try {
            ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree);
        } catch (Exception e) {
            e.printStackTrace();
        }

        diagnosticsCollector.getCollectedDiagnostics().addAll(syntaxErrorListener.getCollectedSyntaxErrors());
        return new ImmutablePair<>(tree, diagnosticsCollector.getCollectedDiagnostics());
    }

    public static BMLParser bmlParser(String inputString) {
        var bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        return new BMLParser(new CommonTokenStream(bmlLexer));
    }

    public static void drawParseTree(BMLParser bmlParser) {
        JFrame frame = new JFrame("BML AST");
        JPanel panel = new JPanel();
        TreeViewer viewer = new TreeViewer(Arrays.asList(bmlParser.getRuleNames()), bmlParser.program());
        viewer.setScale(1);
        panel.add(viewer);
        frame.add(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        bmlParser.reset();
    }
}
