package i5.bml.parser.errors;

public enum ParserError {

    EXPECTED_BUT_FOUND("Expected %s\nFound %s"),
    UNKNOWN_TYPE("Unknown type %s"),
    NOT_DEFINED("%s is not defined"),
    NOT_DEFINED_FOR("%s is not defined for %s"),
    INCOMPATIBLE("%s %s %s is not compatible");

    public final String message;

    public String format(Object... args) {
        return message.formatted(args);
    }

    ParserError(String message) {
        this.message = message;
    }
}
