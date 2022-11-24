package types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

import java.math.BigDecimal;

@BMLType(index = 3, typeString = "Number")
public class BMLNumeric extends AbstractBMLType {

    private BigDecimal value;

    private boolean isFloatingPoint;


    public BMLNumeric() {}

    public BMLNumeric(boolean isFloatingPoint) {
        this.isFloatingPoint = isFloatingPoint;
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }
}
