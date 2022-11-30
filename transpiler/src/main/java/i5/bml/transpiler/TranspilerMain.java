package i5.bml.transpiler;

import i5.bml.parser.Parser;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TranspilerMain {
    public static void main(String[] args) {
        var fileName = "Example.bml";
//        var fileName = "OpenAPIPetStoreWithTelegramExample.bml";
//        var fileName = "ExampleAutomaton.bml";
        var inputString = "";
        try {
            var inputResource = Objects.requireNonNull(TranspilerMain.class.getClassLoader().getResource(fileName));
            inputString = Files.readString(Paths.get(inputResource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var pair = Parser.parse(inputString);
        Parser.makeParseTree(pair.getRight());

        DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                System.err.println(diagnostic.getMessage());
            }
        }
    }
}
