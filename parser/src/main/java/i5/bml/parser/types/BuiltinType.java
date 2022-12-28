package i5.bml.parser.types;

public enum BuiltinType {
    /*
     * Primitives
     */
    NUMBER("Number"),
    LONG_NUMBER("Long Number"),
    FLOAT_NUMBER("Float Number", true),
    BOOLEAN("Boolean"),
    STRING("String"),

    /*
     * Generic types
     */
    OBJECT("Object", true),

    /*
     * Functions
     */
    FUNCTION("Function", true),
    CONTEXT("Context", true),
    VOID("Void", true),

    /*
     * Annotations
     */
    MESSENGER_ANNOTATION("MessengerAnnotation", true),
    ACTION_ANNOTATION("ActionAnnotation", true),
    ROUTINE_ANNOTATION("RoutineAnnotation", true),

    /*
     * Data structures
     */
    LIST("List"),
    MAP("Map"),

    /*
     * Messenger
     */
    TELEGRAM("Telegram"),
    SLACK("Slack"),
    ROCKET_CHAT("RocketChat"),
    HANGOUTS("Hangouts"),
    DISCORD("Discord"),
    USER("User", true),

    /*
     * Dialogue automaton
     */
    DIALOGUE("Dialogue"),
    STATE("State", true),

    /*
     * NLU
     */
    RASA("Rasa"),

    /*
     * Social Networks
     */
    TWITTER("Twitter"),

    /*
     * Services
     */
    OPENAPI("OpenAPI"),
    OPENAPI_SCHEMA("OpenAPISchema", true),
    EMAIL("Email"),
    RSS("RSS"),
    SQL("SQL");

    private final String name;

    private boolean isInternal = false;

    BuiltinType(String name) {
        this.name = name;
    }

    BuiltinType(String name, boolean isInternal) {
        this.name = name;
        this.isInternal = isInternal;
    }

    public boolean isInternal() {
        return isInternal;
    }

    @Override
    public String toString() {
        return name;
    }
}
