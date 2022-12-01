package i5.bml.parser.types;

public enum BuiltinType {
    /*
     * Primitives
     */
    NUMBER("Number"),
    FLOAT_NUMBER("Float Number", true),
    BOOLEAN("Boolean"),
    STRING("String"),

    /*
     * Generic type
     */
    OBJECT("Object"),

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

    /*
     * Dialogue automaton
     */
    DIALOGUE("Dialogue"),

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
    SQL("SQL"),

    /*
     * Timer
     */
    ROUTINE("Routine");

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
