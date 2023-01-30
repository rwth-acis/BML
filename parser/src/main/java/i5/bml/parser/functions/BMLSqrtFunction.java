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
@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "sqrt")
public class BMLSqrtFunction implements BMLFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        var numberParameter = new BMLFunctionParameter("number", TypeRegistry.resolveType(BuiltinType.NUMBER));
        var sqrtSymbol = new VariableSymbol(getName());
        sqrtSymbol.setType(new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.NUMBER), List.of(numberParameter), new ArrayList<>()));
        scope.define(sqrtSymbol);
    }
}
