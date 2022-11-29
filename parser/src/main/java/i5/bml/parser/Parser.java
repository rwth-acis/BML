package i5.bml.parser;

import generatedParser.BMLLexer;
import generatedParser.BMLParser;
import i5.bml.parser.errors.SyntaxErrorListener;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

public class Parser {

    public static List<Diagnostic> parseAndCollectDiagnostics(String inputString, StringBuilder report) {
        var start = System.nanoTime();
        var bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        var bmlParser = new BMLParser(new CommonTokenStream(bmlLexer));
        var syntaxErrorListener = new SyntaxErrorListener();
        bmlParser.removeErrorListeners();
        bmlParser.addErrorListener(syntaxErrorListener);
        var end = System.nanoTime();
        Measurements.add("Parsing (%s Bytes)".formatted(inputString.getBytes().length), (end - start));

//        JFrame frame = new JFrame("BML AST");
//        JPanel panel = new JPanel();
//        TreeViewer viewer = new TreeViewer(Arrays.asList(bmlParser.getRuleNames()), bmlParser.program());
//        viewer.setScale(1);
//        panel.add(viewer);
//        frame.add(panel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.pack();
//        frame.setVisible(true);
//
//        bmlParser.reset();

        DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector();
        start = System.nanoTime();
        new ParseTreeWalker().walk(diagnosticsCollector, bmlParser.program());
        end = System.nanoTime();
        Measurements.add("Collect diagnostics", "Fetch OpenAPI Spec", (end - start));
        Measurements.print(report);

        diagnosticsCollector.getCollectedDiagnostics().addAll(syntaxErrorListener.getCollectedSyntaxErrors());
        return diagnosticsCollector.getCollectedDiagnostics();
    }

    public static Pair<BMLLexer, BMLParser> parse(String inputString) {
        var bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        var bmlParser = new BMLParser(new CommonTokenStream(bmlLexer));

        return new ImmutablePair<>(bmlLexer, bmlParser);
    }
}
