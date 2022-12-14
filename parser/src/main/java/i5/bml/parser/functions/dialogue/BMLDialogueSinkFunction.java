package i5.bml.parser.functions.dialogue;

import i5.bml.parser.functions.BMLFunctionAnnotation;
import i5.bml.parser.functions.BMLFunctionScope;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;

import java.util.ArrayList;

@BMLFunctionAnnotation(scope = BMLFunctionScope.DIALOGUE, name = "sink")
public class BMLDialogueSinkFunction extends AbstractBMLDialogueFunction {

    @Override
    public void defineFunction(Scope scope) {
        super.defineFunction(scope);

        symbol.setType(new BMLFunctionType(stateReturnType, new ArrayList<>(), optionalParameters));
        scope.define(symbol);
    }
}
