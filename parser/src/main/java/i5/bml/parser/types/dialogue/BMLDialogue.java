package i5.bml.parser.types.dialogue;

import i5.bml.parser.types.*;
import org.antlr.symtab.Type;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

@BMLType(name = BuiltinType.DIALOGUE, isComplex = true)
public class BMLDialogue extends AbstractBMLType {

    @Override
    public void initializeType(ParserRuleContext ctx) {
        var contextParameter = new BMLFunctionParameter("context", TypeRegistry.resolveComplexType(BuiltinType.CONTEXT));
        var stepFunctionType = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), List.of(contextParameter), new ArrayList<>());
        supportedAccesses.put("step", stepFunctionType);
    }
}
