package i5.bml.parser.types.components.messenger;

import i5.bml.parser.types.*;

@BMLType(name = BuiltinType.SLACK, isComplex = false)
public class BMLSlackComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "botToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botToken;

    @BMLComponentParameter(name = "appToken", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String appToken;

    public String getBotToken() {
        return botToken;
    }

    public String getAppToken() {
        return appToken;
    }
}
