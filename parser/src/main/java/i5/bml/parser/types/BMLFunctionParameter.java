package i5.bml.parser.types;

import generatedParser.BMLParser;
import org.antlr.symtab.ParameterSymbol;

public class BMLFunctionParameter extends ParameterSymbol {

    private BMLParser.ExpressionContext exprCtx;

    public BMLFunctionParameter(String name) {
        super(name);
    }

    public BMLParser.ExpressionContext getExprCtx() {
        return exprCtx;
    }

    public void setExprCtx(BMLParser.ExpressionContext exprCtx) {
        this.exprCtx = exprCtx;
    }
}
