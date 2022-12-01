package i5.bml.parser.types;

import java.math.BigDecimal;

@BMLType(name = "Number", isComplex = false)
public class BMLNumber extends AbstractBMLType {

    @BMLComponentParameter(name = "value", expectedBMLType = "Number", isRequired = false)
    private BigDecimal value;

    private boolean isFloatingPoint;

    public BMLNumber() {
    }

    public BMLNumber(boolean isFloatingPoint) {
        this.isFloatingPoint = isFloatingPoint;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }

    @Override
    public String toString() {
        return "%s%s".formatted(isFloatingPoint ? "Float " : "", super.getName());
    }

    @Override
    public String encodeToString() {
        return toString();
    }
}
