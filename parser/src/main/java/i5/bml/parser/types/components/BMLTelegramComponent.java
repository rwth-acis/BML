package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLComponentParameter;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.utils.Utils;
import i5.bml.parser.walker.DiagnosticsCollector;

@BMLType(name = BuiltinType.TELEGRAM, isComplex = true)
public class BMLTelegramComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "botName", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botName;

    @BMLComponentParameter(name = "botToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botToken;

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        botName = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "botName");
        botToken = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "botToken");
    }

    public String getBotName() {
        return botName;
    }

    public String getBotToken() {
        return botToken;
    }
}
