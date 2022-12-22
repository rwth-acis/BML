package i5.bml.parser.types;

@BMLType(name = BuiltinType.SLACK, isComplex = false)
public class BMLSlackComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "token", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String token;
}
