package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.types.*;
import i5.bml.parser.walker.DiagnosticsCollector;

@BMLType(name = BuiltinType.SLACK, isComplex = false)
public class BMLSlackComponent extends AbstractBMLType implements CanPopulateParameters {

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

        botToken = extractConstFromRequiredParameter(diagnosticsCollector, ctx, "botToken", false);
        appToken = extractConstFromRequiredParameter(diagnosticsCollector, ctx, "appToken", false);
    }

    public String getBotToken() {
        return botToken;
    }

    public String getAppToken() {
        return appToken;
    }
}
