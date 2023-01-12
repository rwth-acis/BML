package i5.bml.parser.types.components.messenger;

import i5.bml.parser.types.*;

@BMLType(name = BuiltinType.TELEGRAM, isComplex = true)
public class BMLTelegramComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "botName", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botName;

    @BMLComponentParameter(name = "botToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botToken;

    public String getBotName() {
        return botName;
    }

    public String getBotToken() {
        return botToken;
    }
}
