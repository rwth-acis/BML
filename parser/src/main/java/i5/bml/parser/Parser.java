package i5.bml.parser;

import generatedParser.BMLLexer;
import generatedParser.BMLParser;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Parser {

    private static DiagnosticsCollector diagnosticsCollector;

    public static List<Diagnostic> parse(String inputString, StringBuilder report) {
        var start = System.nanoTime();
        var bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        var bmlParser = new BMLParser(new CommonTokenStream(bmlLexer));
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

        diagnosticsCollector = new DiagnosticsCollector("");
        start = System.nanoTime();
        new ParseTreeWalker().walk(diagnosticsCollector, bmlParser.program());
        end = System.nanoTime();
        Measurements.add("Collect diagnostics", "Fetch OpenAPI Spec", (end - start));
        Measurements.print(report);

        return diagnosticsCollector.getCollectedDiagnostics();
    }

    public static DiagnosticsCollector getDiagnosticsCollector() {
        return diagnosticsCollector;
    }
}
