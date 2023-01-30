package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "string")
public class BMLStringFunction implements BMLFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        var numberParameter = new BMLFunctionParameter("number", TypeRegistry.resolveType(BuiltinType.NUMBER));
        numberParameter.addType(TypeRegistry.resolveType(BuiltinType.LONG_NUMBER));
        numberParameter.addType(TypeRegistry.resolveType(BuiltinType.FLOAT_NUMBER));
        var stringSymbol = new VariableSymbol(getName());
        var functionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(numberParameter), new ArrayList<>());
        stringSymbol.setType(functionType);
        scope.define(stringSymbol);
    }
}
