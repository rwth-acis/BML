package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLComponentParameter;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.utils.Utils;
import i5.bml.parser.walker.DiagnosticsCollector;

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

    public String getUrl() {
        return url;
    }
}
