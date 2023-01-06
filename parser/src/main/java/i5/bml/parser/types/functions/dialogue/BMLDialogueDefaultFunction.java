package i5.bml.parser.types.functions.dialogue;

import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.functions.BMLFunctionAnnotation;
import i5.bml.parser.types.functions.BMLFunctionScope;
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
