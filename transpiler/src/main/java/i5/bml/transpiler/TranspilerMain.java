package i5.bml.transpiler;

import i5.bml.parser.Parser;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class TranspilerMain {
    public static void main(String[] args) throws IOException, URISyntaxException {
        var fileName = "Example.bml";
//        var fileName = "OpenAPIPetStoreWithTelegramExample.bml";
//        var fileName = "ExampleAutomaton.bml";
        var inputString = "";
        var inputResource = Objects.requireNonNull(TranspilerMain.class.getClassLoader().getResource(fileName));
        inputString = Files.readString(Paths.get(inputResource.toURI()));

        var pair = Parser.parse(inputString);
        //Parser.drawParseTree(pair.getRight());
        ParseTree tree = pair.getRight().program();

        DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector();
        ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree);
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                System.err.println(diagnostic.getMessage());
            }
        }

        JavaSynthesizer javaSynthesizer = new JavaSynthesizer();
        javaSynthesizer.visit(tree);
    }
}
