package i5.bml.parser.errors;

/**
 * Enum representing possible parser errors. Each error has a corresponding message that can be formatted with provided arguments.
 * The ParserError enum represents error codes for a parser.
 * Each enum value has a predefined message, and a format method that takes in arguments to replace placeholders in the message.
 * <p>
 * Example usage:
 * <pre>
 *     String message = ParserError.UNKNOWN_ANNOTATION.format("myUnknownAnnotation");
 * </pre>
 * The message in this example would be "Unknown annotation `myUnknownAnnotation`".
 */
public enum ParserError {

    /**
     * Error thrown when an expected token is not found.
     */
    EXPECTED_BUT_FOUND("Expected %s\nFound %s"),

    /**
     * Error thrown when an unknown type is encountered.
     */
    UNKNOWN_TYPE("Unknown type %s"),

    /**
     * Error thrown when an identifier is used but not defined in the current scope.
     */
    NOT_DEFINED("%s is not defined"),

    /**
     * Error thrown when the "forEach" statement is not applicable to a given type.
     */
    FOREACH_NOT_APPLICABLE("forEach not applicable to %s"),

    /**
     * Error thrown when initializing a list with non-homogeneous types.
     */
    LIST_BAD_TYPES("List initialization requires homogeneous types"),

    /**
     * Error thrown when expressions in a ternary operator do not have the same type.
     */
    TERNARY_BAD_TYPES("Expressions need to have the same type\nFound %s : %s"),

    /**
     * Error thrown when a given expression is not a statement.
     */
    NOT_A_STATEMENT("Not a statement"),

    /**
     * Error thrown when a parameter is used but not defined in the current scope.
     */
    PARAM_NOT_DEFINED("Parameter %s is not defined"),

    /**
     * Error thrown when an identifier is not defined for a given type.
     */
    NOT_DEFINED_FOR("%s is not defined for %s"),

    /**
     * Error thrown when an identifier is already defined in the current scope.
     */
    ALREADY_DEFINED("%s is already defined in scope"),

    /**
     * Error thrown when an annotation is already used for a function.
     */
    DUP_ANNOTATION("%s is already annotated for function"),

    /**
     * Error thrown when a required parameter is missing.
     */
    MISSING_PARAM("Missing parameter %s"),

    /**
     * Error thrown when an identifier cannot be resolved in a given context.
     */
    CANT_RESOLVE_IN("Can't resolve %s in %s"),

    /**
     * Error thrown when an operator cannot be applied to given types.
     */
    CANNOT_APPLY_OP("Operator %s can't be applied to %s, %s"),

    /**
     * Error thrown when a given string is not a valid URL.
     */
    URL_NOT_VALID("'%s' is not a valid URL"),

    /**
     * Error thrown when the connection to a given URL fails.
     */
    CONNECT_FAILED("Could not connect to url %s"),

    /**
     * Error thrown when a given path is not defined for an API.
     */
    NO_PATH_FOR_API("Path %s is not defined for API:\n%s"),

    /**
     * Error thrown when a given HTTP method is not supported for a path in an API.
     */
    METHOD_NOT_SUPPORTED("Path %s does not support HTTP method %s for API:\n%s"),

    /**
     * Error thrown when a parameter requires a constant value.
     */
    PARAM_REQUIRES_CONSTANT("Parameter `%s` requires a constant `%s`"),

    /**
     * Error thrown when an unknown annotation is encountered.
     */
    UNKNOWN_ANNOTATION("Unknown annotation `%s`"),

    /**
     * Error thrown when trying to assign a value to a global variable.
     */
    CANT_ASSIGN_GLOBAL("Can't assign a global variable"),

    /**
     * Error thrown when trying to assign the result of an expression that returns void.
     */
    CANT_ASSIGN_VOID("Can't assign expression returning `void`"),

    /**
     * Error thrown when the parser expected a specific type, but finds a different type.
     */
    EXPECTED_ANY_OF_1("Expected any of `%s` but found `%s`"),

    /**
     * Error thrown when the parser expects one of 2 types, but finds a different type.
     */
    EXPECTED_ANY_OF_2("Expected any of `%s`, `%s` but found `%s`");

    public final String message;

    public String format(Object... args) {
        return message.formatted(args);
    }

    ParserError(String message) {
        this.message = message;
    }
}
