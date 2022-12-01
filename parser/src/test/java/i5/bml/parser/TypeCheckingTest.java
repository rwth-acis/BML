package i5.bml.parser;

import generatedParser.BMLParser;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.utils.TestUtils;
import i5.bml.parser.utils.TypeCheckWalker;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static i5.bml.parser.errors.ParserError.CANNOT_APPLY_OP;
import static i5.bml.parser.errors.ParserError.EXPECTED_BUT_FOUND;

class TypeCheckingTest {

    // File path relative to test resources folder
    private static final String TYPE_CHECKING_BASE_PATH = "type-checking/";

    @BeforeAll
    static void fileHasCorrectSyntax() {
        var dirPath = "src/test/resources/" + TYPE_CHECKING_BASE_PATH;
        var match = Stream.of(Objects.requireNonNull(new File(dirPath).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(f -> TestUtils.collectSyntaxErrors(TYPE_CHECKING_BASE_PATH + f.getName()))
                .allMatch(List::isEmpty);

        Assertions.assertTrue(match);
    }

    @Test
    void typeCheckAnnotations() {
        TestUtils.assertNoErrors(TYPE_CHECKING_BASE_PATH + "annotations.bml", List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.STRING, BuiltinType.NUMBER)
        ));
    }

    @Test
    void typeCheckArithmetic() {
        TestUtils.assertNoErrors(TYPE_CHECKING_BASE_PATH + "arithmetic.bml", List.of(
                CANNOT_APPLY_OP.format("+", BuiltinType.BOOLEAN, BuiltinType.NUMBER),
                CANNOT_APPLY_OP.format("+", BuiltinType.NUMBER, BuiltinType.STRING),
                CANNOT_APPLY_OP.format("-", "List<String>", BuiltinType.STRING),
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, "List<Number>")
        ));
    }

    @Test
    void typeCheckAssignments() {
        TestUtils.assertNoErrors(TYPE_CHECKING_BASE_PATH + "assignments.bml",List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, BuiltinType.STRING),
                EXPECTED_BUT_FOUND.format("List<String>", "List<Number>")
        ));
    }

    @Test
    void typeCheckBools() {
        TestUtils.assertNoErrors(TYPE_CHECKING_BASE_PATH + "bools.bml",List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, BuiltinType.NUMBER),
                EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, BuiltinType.STRING)
        ));
    }

    @Test
    void typeCheckComponents() {
        TestUtils.assertNoErrors(TYPE_CHECKING_BASE_PATH + "components.bml",List.of(
                "Url ':/petstore3.swagger.io/api/v3/openapi.json' is not valid",
                "Could not connect to url `:/petstore3.swagger.io/api/v3/openapi.json`",
                "Missing parameter `url`",
                "Parameter `link` is not defined",
                EXPECTED_BUT_FOUND.format(BuiltinType.STRING, BuiltinType.NUMBER),
                "Missing parameter `url`",
                "Parameter `id` is not defined"
        ));
    }

    @Test
    void testFloatingPointConversion() {
        var pair = Parser.parse(TestUtils.readFileIntoString(TYPE_CHECKING_BASE_PATH + "arithmetic.bml"));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, pair.getRight().program());
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        final boolean[] floatConversionWasDone = new boolean[]{false};
        final boolean[] floatConversionWasNotDone = new boolean[]{false};
        var typeCheckWalker = new TypeCheckWalker((currentScope, ctx) -> {
            var name = ((BMLParser.AssignmentContext) ctx).name.getText();
            if (name.equals("c4")) {
                var symbol = currentScope.resolve(name);
                floatConversionWasDone[0] = ((VariableSymbol) symbol).getType().equals(TypeRegistry.resolveType(BuiltinType.FLOAT_NUMBER));
            } else if (name.equals("c5")) {
                var symbol = currentScope.resolve(name);
                floatConversionWasNotDone[0] = ((VariableSymbol) symbol).getType().equals(TypeRegistry.resolveType(BuiltinType.NUMBER));
            }
        });

        pair.getRight().reset();
        new ParseTreeWalker().walk(typeCheckWalker, pair.getRight().program());

        Assertions.assertTrue(floatConversionWasDone[0], () -> "Found diagnostics:\n%s".formatted(TestUtils.prettyPrintDiagnostics(diagnostics)));
        Assertions.assertTrue(floatConversionWasNotDone[0], () -> "Found diagnostics:\n%s".formatted(TestUtils.prettyPrintDiagnostics(diagnostics)));
    }
}
