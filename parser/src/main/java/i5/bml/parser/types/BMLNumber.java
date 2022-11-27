package i5.bml.parser.types;

import org.antlr.symtab.ParameterSymbol;

import java.math.BigDecimal;

@BMLType(name = "Number", isComplex = false)
public class BMLNumber extends AbstractBMLType {

    private BigDecimal value;

    private boolean isFloatingPoint;

    public BMLNumber() {
        var p = new ParameterSymbol("value");
        p.setType(TypeRegistry.resolvePrimitiveType("Number"));
        requiredParameters.add(p);
    }

    public BMLNumber(boolean isFloatingPoint) {
        this.isFloatingPoint = isFloatingPoint;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }

    @Override
    public String toString() {
        return "%s %s".formatted(isFloatingPoint ? "Decimal" : "Integer", super.toString());
    }
}
