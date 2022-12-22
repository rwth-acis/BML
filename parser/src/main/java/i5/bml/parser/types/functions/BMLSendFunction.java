package i5.bml.parser.types.functions;

import i5.bml.parser.types.BMLFunctionParameter;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "send")
public class BMLSendFunction implements BMLFunction {

    @Override
    public void defineFunction(Scope scope) {
        var textParameter = new BMLFunctionParameter("text", TypeRegistry.resolveType(BuiltinType.STRING));
        var sendSymbol = new VariableSymbol(getName());
        sendSymbol.setType(new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), List.of(textParameter), new ArrayList<>()));
        scope.define(sendSymbol);
    }
}
