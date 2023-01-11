package i5.bml.parser;

import i5.bml.parser.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static i5.bml.parser.errors.ParserError.*;

class ScopeTest {

    // File path relative to test resources folder
    private static final String WRONG_SCOPES_BML = "WrongScopes.bml";
    private static final String CORRECT_SCOPES_BML = "CorrectScopes.bml";

    @BeforeEach
    void clearCache() {
        TestUtils.clearRegistries();
    }

    @BeforeAll
    static void fileHasCorrectSyntax() {
        Assertions.assertTrue(TestUtils.collectSyntaxErrors(WRONG_SCOPES_BML).isEmpty());
        Assertions.assertTrue(TestUtils.collectSyntaxErrors(CORRECT_SCOPES_BML).isEmpty());
    }

    @Test
    void testWrongScopes() {
        TestUtils.assertErrors(WRONG_SCOPES_BML, List.of(
                NOT_DEFINED.format("d"),
                NOT_DEFINED.format("z"),
                ALREADY_DEFINED.format("rate"),
                CANT_ASSIGN_GLOBAL.message
        ));
    }

    @Test
    void testCorrectScopes() {
        TestUtils.assertErrors(CORRECT_SCOPES_BML, List.of());
    }
}
