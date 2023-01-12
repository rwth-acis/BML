package i5.bml.parser.types.components;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;

@BMLType(name = BuiltinType.CONTEXT, isComplex = true)
public class BMLContext extends AbstractBMLType {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
}
