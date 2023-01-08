package i5.bml.parser.types.annotations;


import i5.bml.parser.types.*;
import i5.bml.parser.types.components.BMLList;
import org.antlr.v4.runtime.ParserRuleContext;

@BMLType(name = BuiltinType.MESSENGER_ANNOTATION, isComplex = true)
public class BMLMessengerAnnotation extends AbstractBMLType {

    @Override
    public void initializeType(ParserRuleContext ctx) {
        supportedAccesses.put("intent", TypeRegistry.resolveType(BuiltinType.STRING));
        var stringListType = TypeRegistry.resolveType("List{itemType=String}");
        if (stringListType == null) {
            stringListType = new BMLList(TypeRegistry.resolveType(BuiltinType.STRING));
            TypeRegistry.registerType(stringListType);
        }
        supportedAccesses.put("entities", stringListType);
        supportedAccesses.put("entity", TypeRegistry.resolveType(BuiltinType.STRING));
        supportedAccesses.put("user", TypeRegistry.resolveType(BuiltinType.USER));
    }
}
