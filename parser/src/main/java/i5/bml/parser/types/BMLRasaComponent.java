package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.utils.Utils;
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

        url = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "url");
    }

    @Override
    public String encodeToString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }
}
