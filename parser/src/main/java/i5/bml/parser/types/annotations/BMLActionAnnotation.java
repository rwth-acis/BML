package i5.bml.parser.types.annotations;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.components.BMLList;
import org.antlr.v4.runtime.ParserRuleContext;

@BMLType(name = BuiltinType.ACTION_ANNOTATION, isComplex = true)
public class BMLActionAnnotation extends AbstractBMLType {

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
