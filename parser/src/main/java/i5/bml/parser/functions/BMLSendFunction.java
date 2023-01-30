package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "send")
public class BMLSendFunction implements BMLFunction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void defineFunction(Scope scope) {
        var textParameter = new BMLFunctionParameter("text", TypeRegistry.resolveType(BuiltinType.STRING));
        var receiverParameter = new BMLFunctionParameter("receiver", TypeRegistry.resolveType(BuiltinType.USER));
        var sendSymbol = new VariableSymbol(getName());
        var functionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), List.of(textParameter), List.of(receiverParameter));
        sendSymbol.setType(functionType);
        scope.define(sendSymbol);
    }
}
