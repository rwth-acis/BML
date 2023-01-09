package i5.bml.parser.utils;

import i5.bml.parser.Parser;
import i5.bml.parser.errors.SyntaxErrorListener;
import i5.bml.parser.walker.DiagnosticsCollector;
import io.swagger.v3.parser.exception.ReadContentException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

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
        try {
            new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        } catch (RecognitionException e) {
            LOGGER.error("Walking parse tree of file {} failed", fileName, e);
        } catch (RuntimeException ignore) {}
        return syntaxErrorListener.getCollectedSyntaxErrors();
    }

    public static void assertErrors(String relativeFilePath, List<String> expectedErrors) {
        var pair = Parser.parse(TestUtils.readFileIntoString(relativeFilePath));
        var diagnosticsCollector = new DiagnosticsCollector();
        try {
            new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        } catch (Exception e) {
            LOGGER.error("Walking parse tree of file {} failed", relativeFilePath, e);
        }

        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();
        diagnostics.removeIf(d -> d.getSeverity() != DiagnosticSeverity.Error);

        expectedErrors.forEach(e -> {
            Assertions.assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().equals(e)),
                    () -> "Expected error: %s\nFound instead:\n%s:".formatted(e, TestUtils.prettyPrintDiagnostics(diagnostics)));
        });
        diagnostics.removeIf(d -> expectedErrors.contains(d.getMessage()));

        Assertions.assertTrue(diagnostics.isEmpty(), () -> "Unexpected diagnostics:\n%s".formatted(TestUtils.prettyPrintDiagnostics(diagnostics)));
    }

    public static String prettyPrintDiagnostics(List<Diagnostic> diagnostics) {
        return diagnostics.stream()
                .map(d -> "line=%d: %s".formatted(d.getRange().getStart().getLine(), d.getMessage()))
                .collect(Collectors.joining("\n"));
    }
}
