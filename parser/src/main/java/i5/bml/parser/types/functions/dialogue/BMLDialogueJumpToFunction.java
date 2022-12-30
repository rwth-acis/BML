package i5.bml.parser.types.functions.dialogue;

import i5.bml.parser.types.BMLFunctionParameter;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.functions.BMLFunctionAnnotation;
import i5.bml.parser.types.functions.BMLFunctionScope;
import org.antlr.symtab.Scope;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.DIALOGUE, name = "jumpTo")
public class BMLDialogueJumpToFunction extends AbstractBMLDialogueFunction {

    @Override
    public void defineFunction(Scope scope) {
        super.defineFunction(scope);

        symbol.setType(new BMLFunctionType(stateReturnType, new ArrayList<>(), List.of(new BMLFunctionParameter("state", stateReturnType))));
        scope.define(symbol);
    }
}
