package i5.bml.parser.types;

@BMLType(name = BuiltinType.BOT, isComplex = false)
public class BMLBot extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "host", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String host;

    @BMLComponentParameter(name = "port", expectedBMLType = BuiltinType.NUMBER, isRequired = true)
    private String port;

    @BMLComponentParameter(name = "name", expectedBMLType = BuiltinType.STRING, isRequired = false)
    private String name;
}
