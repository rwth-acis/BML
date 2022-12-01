package i5.bml.parser;

import i5.bml.parser.utils.TestUtils;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTest {

    // File path relative to test resources folder
    private static final String ALLOWED_SYNTAX_BML = "AllowedSyntax.bml";

    @Test
    void checkAllowedSyntax() {
        var pair = Parser.parse(TestUtils.readFileIntoString(ALLOWED_SYNTAX_BML));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        Assertions.assertTrue(diagnostics.isEmpty(), () -> TestUtils.prettyPrintDiagnostics(diagnostics));
    }
}
