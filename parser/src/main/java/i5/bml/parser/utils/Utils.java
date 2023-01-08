package i5.bml.parser.utils;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.walker.DiagnosticsCollector;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

public class Utils {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static String extractConstStringFromParameter(DiagnosticsCollector diagnosticsCollector,
                                                         BMLParser.ElementExpressionPairListContext ctx,
                                                         String name) {
        var expr = ctx.elementExpressionPair().stream().filter(p -> p.name.getText().equals(name)).findAny().get().expr;
        var atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format(name, BuiltinType.STRING), expr);
            return null;
        } else {
            return atom.getText().substring(1, atom.getText().length() - 1);
        }
    }
}
