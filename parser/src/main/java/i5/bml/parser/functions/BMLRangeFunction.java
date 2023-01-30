package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.primitives.BMLList;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.List;

/**
 * TODO
 */
@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "range")
public class BMLRangeFunction implements BMLFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        var startParameter = new BMLFunctionParameter("start", TypeRegistry.resolveType(BuiltinType.NUMBER));
        var endParameter = new BMLFunctionParameter("end", TypeRegistry.resolveType(BuiltinType.NUMBER));
        var stepParameter = new BMLFunctionParameter("step", TypeRegistry.resolveType(BuiltinType.NUMBER));

        var rangeSymbol = new VariableSymbol(getName());
        var numberListType = TypeRegistry.resolveType("List{itemType=Number}");
        if (numberListType == null) {
            numberListType = new BMLList(TypeRegistry.resolveType(BuiltinType.NUMBER));
            TypeRegistry.registerType(numberListType);
        }
        rangeSymbol.setType(new BMLFunctionType(numberListType, List.of(startParameter, endParameter), List.of(stepParameter)));
        scope.define(rangeSymbol);
    }
}
