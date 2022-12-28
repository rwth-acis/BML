package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

@BMLType(name = BuiltinType.RASA, isComplex = false)
public class BMLRasaComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

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
                    PARAM_REQUIRES_CONSTANT.format("url", BuiltinType.STRING), expr);
        } else {
            url = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }

    @Override
    public String encodeToString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }
}
