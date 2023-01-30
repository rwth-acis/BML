package i5.bml.parser.functions.dialogue;

import i5.bml.parser.functions.BMLFunctionAnnotation;
import i5.bml.parser.functions.BMLFunctionScope;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;

import java.util.ArrayList;

/**
 * The BMLDialogueStateFunction class implements a BML function for creating a state.
 * It inherits its optional parameters from {@link AbstractBMLDialogueFunction}. These parameters are {@code intent} and
 * {@code action}.
 */
@BMLFunctionAnnotation(scope = BMLFunctionScope.DIALOGUE, name = "state")
public class BMLDialogueStateFunction extends AbstractBMLDialogueFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        super.defineFunction(scope);

        symbol.setType(new BMLFunctionType(stateReturnType, new ArrayList<>(), optionalParameters));
        scope.define(symbol);
    }
}
