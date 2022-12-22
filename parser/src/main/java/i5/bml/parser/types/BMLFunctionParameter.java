package i5.bml.parser.types;

import generatedParser.BMLParser;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.ArrayList;
import java.util.List;

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
}
