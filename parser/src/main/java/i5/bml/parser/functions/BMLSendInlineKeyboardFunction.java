package i5.bml.parser.functions;

import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.primitives.BMLList;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;

import java.util.List;

@BMLFunctionAnnotation(scope = BMLFunctionScope.GLOBAL, name = "sendInlineKeyboard")
public class BMLSendInlineKeyboardFunction implements BMLFunction {

    @Override
    public void defineFunction(Scope scope) {
        var textParameter = new BMLFunctionParameter("text", TypeRegistry.resolveType(BuiltinType.STRING));
        var ListOfStringListType = new BMLList(new BMLList(TypeRegistry.resolveType(BuiltinType.STRING)));
        if (TypeRegistry.resolveType(ListOfStringListType) == null) {
            TypeRegistry.registerType(ListOfStringListType);
        }
        var keyboardButtonsParameter = new BMLFunctionParameter("buttonRows", ListOfStringListType);
        var receiverParameter = new BMLFunctionParameter("receiver", TypeRegistry.resolveType(BuiltinType.USER));
        var sendInlineKeyboardSymbol = new VariableSymbol(getName());
        var functionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(textParameter, keyboardButtonsParameter), List.of(receiverParameter));
        sendInlineKeyboardSymbol.setType(functionType);
        scope.define(sendInlineKeyboardSymbol);
    }
}
