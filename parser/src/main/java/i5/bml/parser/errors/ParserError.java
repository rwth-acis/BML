package i5.bml.parser.errors;

public enum ParserError {

    EXPECTED_BUT_FOUND("Expected `%s`\nFound `%s`"),
    UNKNOWN_TYPE("Unknown type `%s`"),
    NOT_DEFINED("`%s` is not defined"),
    FOREACH_NOT_APPLICABLE("forEach not applicable to `%s`"),
    LIST_BAD_TYPES("List initialization requires homogeneous types"),
    TERNARY_BAD_TYPES("Expressions need to have the same type\nFound `%s` : `%s`"),
    NOT_A_STATEMENT("Not a statement"),
    PARAM_NOT_DEFINED("Parameter `%s` is not defined"),
    NOT_DEFINED_FOR("`%s` is not defined for `%s`"),
    ALREADY_DEFINED("`%s` is already defined in scope"),
    MISSING_PARAM("Missing parameter `%s`"),
    CANT_RESOLVE_IN("Can't resolve `%s` in `%s`"),
    CANNOT_APPLY_OP("Operator `%s` can't be applied to `%s`, `%s`"),
    URL_NOT_VALID("'%s' is not a valid URL"),
    CONNECT_FAILED("Could not connect to url `%s`"),
    NO_PATH_FOR_API("Path `%s` is not defined for API:\n`%s`"),
    METHOD_NOT_SUPPORTED("Path `%s` does not support HTTP method `%s` for API:\n`%s`"),
    PARAM_REQUIRES_CONSTANT("Parameter `%s` requires a constant `%s`"),
    UNKNOWN_ANNOTATION("Unknown annotation `%s`");

    public final String message;

    public String format(Object... args) {
        return message.formatted(args);
    }

    ParserError(String message) {
        this.message = message;
    }
}
