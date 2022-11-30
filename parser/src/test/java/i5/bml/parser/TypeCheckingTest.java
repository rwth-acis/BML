package i5.bml.parser;

import i5.bml.parser.utils.TestUtils;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static i5.bml.parser.errors.ParserError.EXPECTED_BUT_FOUND;

public class TypeCheckingTest {

    // File path relative to test resources folder
    private static final String TYPE_CHECKING_BASE_PATH = "type-checking/";

    private static Stream<Arguments> typeChecks() {
        return Stream.of(
                Arguments.of(TYPE_CHECKING_BASE_PATH + "annotations.bml", EXPECTED_BUT_FOUND.format("String", "Number"))
        );
    }

    @BeforeAll
    static void fileHasCorrectSyntax() {
        var dirPath = "src/test/resources/" + TYPE_CHECKING_BASE_PATH;
        var match = Stream.of(Objects.requireNonNull(new File(dirPath).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(f -> TestUtils.collectSyntaxErrors(TYPE_CHECKING_BASE_PATH + f.getName()))
                .allMatch(List::isEmpty);

        Assertions.assertTrue(match);
    }

    @ParameterizedTest
    @MethodSource("typeChecks")
    void checkAllowedSyntax(String relativeFilePath, String errorMsg) {
        var pair = Parser.parse(TestUtils.readFileIntoString(relativeFilePath));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());

        var containsMsg = diagnosticsCollector.getCollectedDiagnostics().stream()
                .anyMatch(d -> d.getMessage().equals(errorMsg));

        Assertions.assertTrue(containsMsg);
    }
}
