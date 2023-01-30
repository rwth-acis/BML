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
@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "date")
public class BMLDateFunction implements BMLFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        var formatParameter = new BMLFunctionParameter("format", TypeRegistry.resolveType(BuiltinType.STRING));
        var dateSymbol = new VariableSymbol(getName());
        dateSymbol.setType(new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(formatParameter), new ArrayList<>()));
        scope.define(dateSymbol);
    }
}
