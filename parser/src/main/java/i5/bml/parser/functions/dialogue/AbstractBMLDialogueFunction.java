package i5.bml.parser.functions.dialogue;

import i5.bml.parser.functions.BMLFunction;
import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.primitives.BMLList;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;

import java.util.Arrays;
import java.util.List;

/**
 * The AbstractBMLDialogueFunction abstract class provides a basic implementation of a BML function.
 */
public abstract class AbstractBMLDialogueFunction implements BMLFunction {

    /**
     * The return type of the state.
     */
    protected Type stateReturnType;

    /**
     * A list of optional parameters for the function.
     */
    protected List<BMLFunctionParameter> optionalParameters;

    /**
     * The symbol for the function.
     */
    protected VariableSymbol symbol;

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        stateReturnType = TypeRegistry.resolveComplexType(BuiltinType.STATE);

        var intentParameter = new BMLFunctionParameter("intent");
        intentParameter.setType(TypeRegistry.resolveType(BuiltinType.STRING));

        var actionParameter = new BMLFunctionParameter("action");
        actionParameter.addType(TypeRegistry.resolveType(BuiltinType.STRING));
        var stringListType = TypeRegistry.resolveType("List{itemType=String}");
        if (stringListType == null) {
            stringListType = new BMLList(TypeRegistry.resolveType(BuiltinType.STRING));
            TypeRegistry.registerType(stringListType);
            actionParameter.addType(stringListType);
        }
        actionParameter.addType(TypeRegistry.resolveType(stringListType));
        actionParameter.addType(TypeRegistry.resolveComplexType(BuiltinType.FUNCTION));
        actionParameter.addType(TypeRegistry.resolveComplexType(BuiltinType.STATE));

        optionalParameters = Arrays.asList(intentParameter, actionParameter);

        symbol = new VariableSymbol(getName());
    }
}
