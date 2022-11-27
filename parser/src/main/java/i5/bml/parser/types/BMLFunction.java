package i5.bml.parser.types;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.List;

public class BMLFunction extends AbstractBMLType {

    private final Type returnType;

    public BMLFunction(Type returnType, List<ParameterSymbol> requiredParameters, List<ParameterSymbol> optionalParameters) {
        this.returnType = returnType;
        super.requiredParameters = requiredParameters;
        super.optionalParameters = optionalParameters;
    }

    @Override
    public String getName() {
        return "Function"; // TODO
    }

    @Override
    public int getTypeIndex() {
        return -1; // TODO
    }

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return ((AbstractBMLType) returnType).resolveAccess(ctx);
    }

    public Type getReturnType() {
        return returnType;
    }

}
