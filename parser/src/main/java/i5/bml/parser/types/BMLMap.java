package i5.bml.parser.types;

import org.antlr.symtab.Type;

@BMLType(name = "Map", isComplex = true)
public class BMLMap extends AbstractBMLType {

    public Type getKeyType() {
        return null;
    }

    public Type getValueType() {
        return null;
    }
}
