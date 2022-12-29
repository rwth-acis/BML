package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.utils.Utils;
import i5.bml.parser.walker.DiagnosticsCollector;

@BMLType(name = BuiltinType.SLACK, isComplex = false)
public class BMLSlackComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "botToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botToken;

    @BMLComponentParameter(name = "appToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String appToken;

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        botToken = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "botToken");
        appToken = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "appToken");
    }

    public String getBotToken() {
        return botToken;
    }

    public String getAppToken() {
        return appToken;
    }
}
