package i5.bml.parser.types.components.primitives;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.Summable;

@BMLType(name = BuiltinType.STRING, isComplex = false)
public class BMLString extends AbstractBMLType implements Summable {
}
