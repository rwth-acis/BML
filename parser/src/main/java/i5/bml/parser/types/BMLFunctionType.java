package i5.bml.parser.types;

import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

@BMLType(name = BuiltinType.FUNCTION, isComplex = true)
public class BMLFunctionType extends AbstractBMLType {

    private Type returnType;

    public BMLFunctionType() {
    }

    public BMLFunctionType(Type returnType, List<BMLFunctionParameter> requiredParameters, List<BMLFunctionParameter> optionalParameters) {
        this.returnType = returnType;
        super.requiredParameters = requiredParameters;
        super.optionalParameters = optionalParameters;
    }

    public BMLFunctionType(BMLFunctionType functionType) {
        this.returnType = ((AbstractBMLType) functionType.getReturnType()).deepCopy();

        functionType.getRequiredParameters().forEach(p -> deepCopyParameter(p, super.requiredParameters));

        functionType.getOptionalParameters().forEach(p -> deepCopyParameter(p, super.optionalParameters));
    }

    private void deepCopyParameter(BMLFunctionParameter p, List<BMLFunctionParameter> parameters) {
        BMLFunctionParameter newParameter = new BMLFunctionParameter(p.getName());
        newParameter.superSetType(p.getType());

        for (Type allowedType : p.getAllowedTypes()) {
            newParameter.addType(((AbstractBMLType) allowedType).deepCopy());
        }

        parameters.add(newParameter);
    }

    @Override
    public String getName() {
        return "Function(required=%s, optional=%s) -> %s".formatted(requiredParameters, optionalParameters, returnType);
    }

    @Override
    public void initializeType(ParserRuleContext ctx) {
        ((AbstractBMLType) returnType).initializeType(ctx);
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
