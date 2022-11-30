package i5.bml.parser;

import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.utils.TestUtils;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static i5.bml.parser.errors.ParserError.ALREADY_DEFINED;
import static i5.bml.parser.errors.ParserError.NOT_DEFINED;

class ScopeTest {

    // File path relative to test resources folder
    private static final String WRONG_SCOPES_BML = "WrongScopes.bml";
    private static final String CORRECT_SCOPES_BML = "CorrectScopes.bml";

    @BeforeEach
    void clearCache() {
        TypeRegistry.clear();
        TypeRegistry.init();
    }

    @BeforeAll
    static void fileHasCorrectSyntax() {
        Assertions.assertTrue(TestUtils.collectSyntaxErrors(WRONG_SCOPES_BML).isEmpty());
        Assertions.assertTrue(TestUtils.collectSyntaxErrors(CORRECT_SCOPES_BML).isEmpty());
    }

    private static List<String> wrongScopeErrors() {
        // Arguments follow the structure: (expectedMessage)
        return List.of(
                ALREADY_DEFINED.format("a"),
                ALREADY_DEFINED.format("alreadyDefinedInLocalScope"),
                ALREADY_DEFINED.format("b"),
                NOT_DEFINED.format("d"),
                ALREADY_DEFINED.format("data"),
                NOT_DEFINED.format("z"),
                ALREADY_DEFINED.format("rate")
        );
    }

    @Test
    void testWrongScopes() {
        var pair = Parser.parse(TestUtils.readFileIntoString(WRONG_SCOPES_BML));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());

        var diagnostics = new ArrayList<>(diagnosticsCollector.getCollectedDiagnostics().stream()
                .map(Diagnostic::getMessage)
                .toList());

        Assertions.assertTrue(diagnostics.containsAll(wrongScopeErrors()));
        diagnostics.removeAll(wrongScopeErrors());
        Assertions.assertTrue(diagnostics.isEmpty(), () -> String.join("\n", diagnostics));
    }

    @Test
    void testCorrectScopes() {
        var pair = Parser.parse(TestUtils.readFileIntoString(CORRECT_SCOPES_BML));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics().stream()
                .map(Diagnostic::getMessage)
                .toList();

        Assertions.assertTrue(diagnostics.isEmpty(), () -> String.join("\n", diagnostics));
    }
}
