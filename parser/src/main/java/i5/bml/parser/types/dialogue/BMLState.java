package i5.bml.parser.types.dialogue;

import generatedParser.BMLParser;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;

@BMLType(name = BuiltinType.STATE, isComplex = true)
public class BMLState extends AbstractBMLType {

    private String intent;

    private BMLParser.ExpressionContext action;

    private Type actionType;

    @Override
    public void initializeType(ParserRuleContext ctx) {
        var functionCallContext = (BMLParser.FunctionCallContext) ctx;
        for (var elementExpressionPairContext : functionCallContext.params.elementExpressionPair()) {
            switch (elementExpressionPairContext.name.getText()) {
                case "intent" -> {
                    var atom = elementExpressionPairContext.expr.atom().token.getText();
                    intent = atom.substring(1, atom.length() - 1);
                }
                case "action" -> {
                    action = elementExpressionPairContext.expr;
                    actionType = elementExpressionPairContext.expr.type;
                }
            }
        }
    }

    @Override
    public Type deepCopy() {
        return new BMLState();
    }

    public String getIntent() {
        return intent;
    }

    public BMLParser.ExpressionContext getAction() {
        return action;
    }

    public Type getActionType() {
        return actionType;
    }
}
