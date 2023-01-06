package i5.bml.parser.types.functions.dialogue;

import i5.bml.parser.types.BMLFunctionParameter;
import i5.bml.parser.types.BMLList;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunction;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractBMLDialogueFunction implements BMLFunction {

    protected Type stateReturnType;

    protected List<BMLFunctionParameter> optionalParameters;

    protected VariableSymbol symbol;

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
