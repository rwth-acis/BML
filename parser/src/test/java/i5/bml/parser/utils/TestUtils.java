package i5.bml.parser.utils;

import i5.bml.parser.Parser;
import i5.bml.parser.errors.SyntaxErrorListener;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TestUtils {

    public static String readFileIntoString(String fileName) {
        var inputString = "";
        try {
            var inputResource = Objects.requireNonNull(TestUtils.class.getClassLoader().getResource(fileName));
            inputString = Files.readString(Paths.get(inputResource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return inputString;
    }

    public static List<Diagnostic> collectSyntaxErrors(String fileName) {
        var diagnosticsCollector = new DiagnosticsCollector();
        var syntaxErrorListener = new SyntaxErrorListener();
        var pair = Parser.parse(TestUtils.readFileIntoString(fileName));
        pair.getRight().removeErrorListeners();
        pair.getRight().addErrorListener(syntaxErrorListener);
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        return syntaxErrorListener.getCollectedSyntaxErrors();
    }
}
