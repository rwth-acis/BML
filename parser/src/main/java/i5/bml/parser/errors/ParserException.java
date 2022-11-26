package i5.bml.parser.errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

public class ParserException extends RuntimeException {

    public ParserException(String message, int lineNumber, Interval interval) {
        super(lineNumber + ":[" + interval + "]: " + message);
    }

    public ParserException(String message, ParserRuleContext ctx) {
        this(message, ctx.getStart().getLine(),
                new Interval(ctx.getStart().getCharPositionInLine(), ctx.getStop().getCharPositionInLine()));
    }

    public ParserException(String message, Token token) {
        this(message, token.getLine(),
                new Interval(token.getCharPositionInLine() + 1, token.getCharPositionInLine() + token.getText().length()));
    }
}
