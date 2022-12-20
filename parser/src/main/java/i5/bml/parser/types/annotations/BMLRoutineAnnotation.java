package i5.bml.parser.types.annotations;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLComponentParameter;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.walker.DiagnosticsCollector;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

@BMLType(name = BuiltinType.ROUTINE_ANNOTATION, isComplex = true)
public class BMLRoutineAnnotation extends AbstractBMLType {

    @BMLComponentParameter(name = "rate", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String rate;

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        var expr = ctx.elementExpressionPair(0).expr;
        var atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format("rate", BuiltinType.STRING), expr);
        } else {
            rate = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }
}
