package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "number")
public class BMLNumberFunction implements BMLFunction {

    @Override
    public void defineFunction(Scope scope) {
        var stringParameter = new BMLFunctionParameter("string", TypeRegistry.resolveType(BuiltinType.STRING));
        var numberSymbol = new VariableSymbol(getName());
        numberSymbol.setType(new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.NUMBER), List.of(stringParameter), new ArrayList<>()));
        scope.define(numberSymbol);
    }
}
