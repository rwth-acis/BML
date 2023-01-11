package i5.bml.parser.functions;

import generatedParser.BMLParser;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BMLFunctionParameter extends ParameterSymbol {

    private BMLParser.ExpressionContext exprCtx;

    private final List<Type> allowedTypes = new ArrayList<>();

    public BMLFunctionParameter(String name) {
        super(name);
    }

    public BMLFunctionParameter(String name, Type type) {
        super(name);
        setType(type);
    }

    @Override
    public void setType(Type type) {
        super.setType(type);
        allowedTypes.add(type);
    }

    public void superSetType(Type type) {
        super.setType(type);
    }

    public void addType(Type type) {
        allowedTypes.add(type);
    }

    public BMLParser.ExpressionContext getExprCtx() {
        return exprCtx;
    }

    public void setExprCtx(BMLParser.ExpressionContext exprCtx) {
        this.exprCtx = exprCtx;
    }

    public List<Type> getAllowedTypes() {
        return allowedTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BMLFunctionParameter that = (BMLFunctionParameter) o;

        if (!Objects.equals(exprCtx, that.exprCtx)) return false;
        return allowedTypes.equals(that.allowedTypes);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (exprCtx != null ? exprCtx.hashCode() : 0);
        result = 31 * result + allowedTypes.hashCode();
        return result;
    }
}
