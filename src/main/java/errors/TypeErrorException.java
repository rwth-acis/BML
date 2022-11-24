package errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class TypeErrorException extends ParserException {

    public TypeErrorException(String message, int lineNumber, Interval interval) {
        super(message, lineNumber, interval);
    }

    public TypeErrorException(String message, ParserRuleContext ctx) {
        super(message.formatted(ctx.getText()), ctx.getStart().getLine(),
                new Interval(ctx.getStart().getCharPositionInLine(), ctx.getStop().getCharPositionInLine()));
    }
}
