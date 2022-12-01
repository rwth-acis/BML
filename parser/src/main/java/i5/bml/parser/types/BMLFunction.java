package i5.bml.parser.types;

import i5.bml.parser.walker.DiagnosticsCollector;
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
        // TODO: Improve this
        return "Function<Returns=%s>".formatted(returnType);
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return ((AbstractBMLType) returnType).resolveAccess(diagnosticsCollector, ctx);
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return "BMLFunction{returnType=%s, requiredParameters=%s, optionalParameters=%s}".formatted(returnType, requiredParameters, optionalParameters);
    }

    @Override
    public String encodeToString() {
        return toString();
    }
}
