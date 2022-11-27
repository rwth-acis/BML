package i5.bml.parser;

import generatedParser.BMLLexer;
import generatedParser.BMLParser;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Parser {

    public static void parse(String fileName, String inputString) {
        var start = System.nanoTime();
        BMLLexer bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        CommonTokenStream tokens = new CommonTokenStream(bmlLexer);
        BMLParser bmlParser = new BMLParser(tokens);
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

        ParseTreeWalker walker = new ParseTreeWalker();

        try {
            DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector(fileName);
            start = System.nanoTime();
            walker.walk(diagnosticsCollector, bmlParser.program());
            end = System.nanoTime();
            Measurements.add("Collect diagnostics", "Fetch OpenAPI Spec", (end - start));
            bmlParser.reset();
            Measurements.print();

            for (String diagnostic : diagnosticsCollector.getCollectedDiagnostics()) {
                System.out.println(diagnostic);
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
