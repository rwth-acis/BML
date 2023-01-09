package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

public interface CanPopulateParameters {

    default String extractConstValueFromParameter(DiagnosticsCollector diagnosticsCollector,
                                                  BMLParser.ElementExpressionPairListContext ctx,
                                                  String name, boolean isInteger) {
        var expr = ctx.elementExpressionPair().stream().filter(p -> p.name.getText().equals(name)).findAny();
        if (expr.isEmpty()) {
            return "";
        }

        var atom = expr.get().expr.atom();
        if (atom == null || (!isInteger && atom.StringLiteral() == null) || (isInteger && atom.IntegerLiteral() == null)) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format(name, BuiltinType.STRING), expr.get().expr);
            return "";
        } else {
            if (isInteger) {
                return atom.getText();
            } else {
                return atom.getText().substring(1, atom.getText().length() - 1);
            }
        }
    }
}
