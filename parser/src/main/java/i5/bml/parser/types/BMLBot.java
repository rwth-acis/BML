package i5.bml.parser.types;

@BMLType(name = BuiltinType.BOT, isComplex = false)
public class BMLBot extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "host", expectedBMLType = BuiltinType.STRING, isRequired = false)
    private String host;

    @BMLComponentParameter(name = "port", expectedBMLType = BuiltinType.NUMBER, isRequired = false)
    private String port;

    @BMLComponentParameter(name = "name", expectedBMLType = BuiltinType.STRING, isRequired = false)
    private String name;
}
