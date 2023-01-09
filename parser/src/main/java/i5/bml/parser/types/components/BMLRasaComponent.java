package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.types.*;
import i5.bml.parser.walker.DiagnosticsCollector;

@BMLType(name = BuiltinType.RASA, isComplex = false)
public class BMLRasaComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "url", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String url;

    @Override
    public String encodeToString() {
        return "%s{url='%s'}".formatted(getName(), url);
    }

    public String getUrl() {
        return url;
    }
}
