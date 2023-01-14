package i5.bml.parser.types.components.nlu;

import i5.bml.parser.types.*;

@BMLType(name = BuiltinType.RASA, isComplex = false)
public class BMLRasaComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

    @BMLComponentParameter(name = "trainingFile", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String trainingFileName;

    public String url() {
        return url;
    }

    public String trainingFileName() {
        return trainingFileName;
    }
}
