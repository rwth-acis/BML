package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "string")
public class BMLStringFunction implements BMLFunction {

    private BMLFunctionType functionType;

    @Override
    public void defineFunction(Scope scope) {
        var numberParameter = new BMLFunctionParameter("number", TypeRegistry.resolveType(BuiltinType.NUMBER));
        var stringSymbol = new VariableSymbol(getName());
        functionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(numberParameter), new ArrayList<>());
        stringSymbol.setType(functionType);
        scope.define(stringSymbol);
    }

    public BMLFunctionType getFunctionType() {
        return functionType;
    }
}
