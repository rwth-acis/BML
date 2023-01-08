package i5.bml.parser.types.components;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLComponentParameter;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;

@BMLType(name = BuiltinType.OPENAI, isComplex = true)
public class BMLOpenAI extends AbstractBMLType {

    @BMLComponentParameter(name = "key", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String key;

    @BMLComponentParameter(name = "model", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String model;
}
