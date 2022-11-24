package errors;

import org.antlr.v4.runtime.misc.Interval;

public class ParserException extends RuntimeException {

    public ParserException(String message, int lineNumber, Interval interval) {
        super(lineNumber + ":[" + interval + "]: " + message);
    }
}
