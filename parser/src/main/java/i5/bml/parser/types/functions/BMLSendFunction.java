package i5.bml.parser.types.functions;

import i5.bml.parser.types.BMLFunctionParameter;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "send")
public class BMLSendFunction implements BMLFunction {

    private BMLFunctionType functionType;

    @Override
    public void defineFunction(Scope scope) {
        var textParameter = new BMLFunctionParameter("text", TypeRegistry.resolveType(BuiltinType.STRING));
        var receiverParameter = new BMLFunctionParameter("receiver", TypeRegistry.resolveType(BuiltinType.USER));
        var sendSymbol = new VariableSymbol(getName());
        functionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), List.of(textParameter), List.of(receiverParameter));
        sendSymbol.setType(functionType);
        scope.define(sendSymbol);
    }

    public BMLFunctionType getFunctionType() {
        return functionType;
    }
}
