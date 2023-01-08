package i5.bml.parser.types.components;

import i5.bml.parser.types.*;

@BMLType(name = BuiltinType.NUMBER, isComplex = false)
public class BMLNumber extends AbstractBMLType implements Summable {

    @BMLComponentParameter(name = "value", expectedBMLType = BuiltinType.NUMBER, isRequired = false)
    private int value;

    private boolean isFloatingPoint;

    private boolean isLong;

    public BMLNumber() {
    }

    public BMLNumber(boolean isFloatingPoint) {
        this.isFloatingPoint = isFloatingPoint;
        this.isLong = false;
    }

    public BMLNumber(boolean isFloatingPoint, boolean isLong) {
        this.isFloatingPoint = isFloatingPoint;
        this.isLong = isLong;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }

    public boolean isLong() {
        return isLong;
    }

    @Override
    public String toString() {
        if (isFloatingPoint) {
            return BuiltinType.FLOAT_NUMBER.toString();
        } else if (isLong) {
            return BuiltinType.LONG_NUMBER.toString();
        } else {
            return BuiltinType.NUMBER.toString();
        }
    }

    @Override
    public String encodeToString() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BMLNumber bmlNumber = (BMLNumber) o;

        return isFloatingPoint == bmlNumber.isFloatingPoint;
    }
}
