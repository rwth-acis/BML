package i5.bml.parser;

import generatedParser.BMLParser;
import i5.bml.parser.types.BuiltinAnnotation;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.primitives.BMLNumber;
import i5.bml.parser.utils.TestUtils;
import i5.bml.parser.utils.TypeCheckWalker;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static i5.bml.parser.errors.ParserError.*;

class TypeCheckingTest {

    // File path relative to test resources folder
    private static final String TYPE_CHECKING_BASE_PATH = "type-checking/";

//    @BeforeAll
//    static void fileHasCorrectSyntax() {
//        var dirPath = "src/test/resources/" + TYPE_CHECKING_BASE_PATH;
//        var match = Stream.of(Objects.requireNonNull(new File(dirPath).listFiles()))
//                .filter(file -> !file.isDirectory())
//                .map(f -> TestUtils.collectSyntaxErrors(TYPE_CHECKING_BASE_PATH + f.getName()))
//                .allMatch(List::isEmpty);
//
//        Assertions.assertTrue(match);
//    }

    @Test
    void typeCheckAnnotations() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "annotations.bml", List.of(
                EXPECTED_ANY_OF_1.format(BuiltinType.STRING, BuiltinType.NUMBER),
                DUP_ANNOTATION.format(BuiltinAnnotation.USER_SENT_MESSAGE)
        ));
    }

    @Test
    void typeCheckArithmetic() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "arithmetic.bml", List.of(
                CANNOT_APPLY_OP.format("+", BuiltinType.BOOLEAN, BuiltinType.NUMBER),
                CANNOT_APPLY_OP.format("+", BuiltinType.NUMBER, BuiltinType.STRING),
                CANNOT_APPLY_OP.format("-", "List<String>", BuiltinType.STRING),
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, "List<Number>")
        ));
    }

    @Test
    void testFloatingPointConversion() {
        var parser = Parser.bmlParser(TestUtils.readFileIntoString(TYPE_CHECKING_BASE_PATH + "arithmetic.bml"));
        var diagnosticsCollector = new DiagnosticsCollector();
        new ParseTreeWalker().walk(diagnosticsCollector, parser.program());
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
                floatConversionWasNotDone[0] = ((VariableSymbol) symbol).getType() instanceof BMLNumber;
            }
        });

        parser.reset();
        new ParseTreeWalker().walk(typeCheckWalker, parser.program());

        Assertions.assertTrue(floatConversionWasDone[0], () -> "Found diagnostics:\n%s".formatted(TestUtils.prettyPrintDiagnostics(diagnostics)));
        Assertions.assertTrue(floatConversionWasNotDone[0], () -> "Found diagnostics:\n%s".formatted(TestUtils.prettyPrintDiagnostics(diagnostics)));
    }

    @Test
    void typeCheckAssignments() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "assignments.bml", List.of(
                CANNOT_APPLY_OP.format("+=", BuiltinType.NUMBER, BuiltinType.STRING),
                CANNOT_APPLY_OP.format("+=", "List<String>", "List<Number>"),
                CANNOT_APPLY_OP.format("-=", "List<String>", "List<String>")
        ));
    }

    @Test
    void typeCheckBools() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "bools.bml", List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, BuiltinType.NUMBER),
                EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, BuiltinType.STRING)
        ));
    }

    @Test
    void typeCheckComponents() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "components.bml", List.of(
                CONNECT_FAILED.format(":/petstore3.swagger.io/api/v3/openapi.json"),
                MISSING_PARAM.format("url"),
                PARAM_NOT_DEFINED.format("link"),
                EXPECTED_ANY_OF_1.format(BuiltinType.STRING, BuiltinType.NUMBER),
                MISSING_PARAM.format("url"),
                PARAM_NOT_DEFINED.format("id")
        ));
    }

    @Test
    void typeCheckBotHead() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "correctBotHead.bml", List.of());
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "wrongBotHead.bml", List.of(
                EXPECTED_ANY_OF_1.format(BuiltinType.STRING, BuiltinType.NUMBER),
                EXPECTED_ANY_OF_1.format(BuiltinType.NUMBER, BuiltinType.STRING)
        ));
    }

    @Test
    void typeCheckEqualities() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "equalities.bml", List.of(
                CANNOT_APPLY_OP.format("==", BuiltinType.NUMBER, "List<Number>")
        ));
    }

    @Test
    void typeCheckFieldAccesses() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "fieldAccesses.bml", List.of());
    }

    @Test
    void typeCheckForEach() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "foreach.bml", List.of(
                FOREACH_NOT_APPLICABLE.format(BuiltinType.STRING)
        ));
    }

    @Test
    void typeCheckFunctionCalls() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "functionCalls.bml", List.of(
                EXPECTED_ANY_OF_1.format(BuiltinType.LONG_NUMBER, BuiltinType.STRING),
                PARAM_NOT_DEFINED.format("name"),
                MISSING_PARAM.format("petId"),
                NOT_DEFINED.format("do")
        ));
    }

    @Test
    void typeCheckListAccess() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "listAccess.bml", List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, BuiltinType.FLOAT_NUMBER)
        ));
    }

    @Test
    void typeCheckListInitializer() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "listInitializer.bml", List.of(
                LIST_BAD_TYPES.message
        ));
    }

    @Test
    void typeCheckMapInitializer() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "mapInitializer.bml", List.of(
                ALREADY_DEFINED.format("a"),
                CANT_RESOLVE_IN.format("d", "Map"),
                CANT_RESOLVE_IN.format("get", "Map")
        ));

    }

    @Test
    void typeCheckOpenAPIFunctionCalls() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "openAPIFunctionCalls.bml", List.of(
                NO_PATH_FOR_API.format("/pet/{petId}/get", "https://petstore3.swagger.io/api/v3/openapi.json"),
                METHOD_NOT_SUPPORTED.format("/pet", "get", "https://petstore3.swagger.io/api/v3/openapi.json"),
                MISSING_PARAM.format("path"),
                EXPECTED_BUT_FOUND.format(BuiltinType.STRING, BuiltinType.NUMBER),
                PARAM_REQUIRES_CONSTANT.format("url", BuiltinType.STRING),
                CONNECT_FAILED.format("")
        ));
    }

    @Test
    void typeCheckRelations() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "relations.bml", List.of(
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, BuiltinType.BOOLEAN),
                EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, BuiltinType.STRING),
                CANNOT_APPLY_OP.format(">", BuiltinType.STRING, BuiltinType.BOOLEAN)
        ));
    }

    @Test
    void typeCheckTernary() {
        TestUtils.assertErrors(TYPE_CHECKING_BASE_PATH + "ternary.bml", List.of(
                TERNARY_BAD_TYPES.format(BuiltinType.NUMBER, BuiltinType.STRING),
                TERNARY_BAD_TYPES.format(BuiltinType.NUMBER, BuiltinType.OBJECT),
                TERNARY_BAD_TYPES.format(BuiltinType.STRING, BuiltinType.NUMBER),
                EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, BuiltinType.STRING),
                CANNOT_APPLY_OP.format(">", BuiltinType.STRING, BuiltinType.STRING)
        ));
    }
}
