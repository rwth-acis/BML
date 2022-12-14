package i5.bml.parser;

import i5.bml.parser.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static i5.bml.parser.errors.ParserError.NOT_A_STATEMENT;

class SyntaxTest {

    // File path relative to test resources folder
    private static final String ALLOWED_SYNTAX_BML = "AllowedSyntax.bml";
    private static final String DISALLOWED_STATEMENT_SYNTAX_BML_BASE = "disallowed-syntax/statements/";

    @Test
    void checkAllowedSyntax() {
        TestUtils.assertErrors(ALLOWED_SYNTAX_BML, List.of());
    }

    @Test
    void checkStatements() {
        TestUtils.assertErrors(DISALLOWED_STATEMENT_SYNTAX_BML_BASE + "lonelyExpression.bml", List.of(
                NOT_A_STATEMENT.message
        ));
    }
}
