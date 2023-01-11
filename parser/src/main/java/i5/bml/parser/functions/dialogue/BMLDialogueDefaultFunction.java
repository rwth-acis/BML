package i5.bml.parser.functions.dialogue;

import i5.bml.parser.functions.BMLFunctionAnnotation;
import i5.bml.parser.functions.BMLFunctionScope;
import i5.bml.parser.types.BMLFunctionType;
import org.antlr.symtab.Scope;

import java.util.ArrayList;

@BMLFunctionAnnotation(scope = BMLFunctionScope.DIALOGUE, name = "default")
public class BMLDialogueDefaultFunction extends AbstractBMLDialogueFunction {

    @Override
    public void defineFunction(Scope scope) {
        super.defineFunction(scope);

        symbol.setType(new BMLFunctionType(stateReturnType, new ArrayList<>(), optionalParameters.subList(1, optionalParameters.size())));
        scope.define(symbol);
    }
}
