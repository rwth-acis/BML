package types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.Type;

import java.math.BigDecimal;

@BMLType(index = 3, typeString = "Number")
public class BMLNumeric extends AbstractBMLType {

    private BigDecimal value;


    public BMLNumeric() {}

    public BMLNumeric(String value) {
        this.value = new BigDecimal(value);
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return null;
    }
}
