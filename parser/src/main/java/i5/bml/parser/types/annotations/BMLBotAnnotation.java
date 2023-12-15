package i5.bml.parser.types.annotations;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import org.antlr.v4.runtime.ParserRuleContext;

@BMLType(name = BuiltinType.BOT_ANNOTATION, isComplex = true)
public class BMLBotAnnotation extends AbstractBMLType {

    @Override
    public void initializeType(ParserRuleContext ctx) {}
}
