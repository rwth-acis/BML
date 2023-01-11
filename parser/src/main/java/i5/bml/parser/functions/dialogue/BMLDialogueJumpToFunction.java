package i5.bml.parser.functions.dialogue;

import i5.bml.parser.functions.BMLFunctionAnnotation;
import i5.bml.parser.functions.BMLFunctionScope;
import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.types.BMLFunctionType;
import org.antlr.symtab.Scope;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.DIALOGUE, name = "jumpTo")
public class BMLDialogueJumpToFunction extends AbstractBMLDialogueFunction {

    @Override
    public void defineFunction(Scope scope) {
        super.defineFunction(scope);

        symbol.setType(new BMLFunctionType(stateReturnType, List.of(new BMLFunctionParameter("state", stateReturnType)), new ArrayList<>()));
        scope.define(symbol);
    }
}