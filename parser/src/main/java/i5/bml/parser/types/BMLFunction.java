package i5.bml.parser.types;

import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public class BMLFunction extends AbstractBMLType {

    private final Type returnType;

    public BMLFunction(Type returnType, List<BMLFunctionParameter> requiredParameters, List<BMLFunctionParameter> optionalParameters) {
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

    public List<BMLFunctionParameter> getRequiredParameters() {
        return requiredParameters;
    }

    public List<BMLFunctionParameter> getOptionalParameters() {
        return optionalParameters;
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
