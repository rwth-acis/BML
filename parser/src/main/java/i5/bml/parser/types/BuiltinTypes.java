package i5.bml.parser.types;

public enum BuiltinTypes {
    /*
     * Primitives
     */
    NUMBER,
    BOOLEAN,
    STRING,

    /*
     * Data structures
     */
    LIST,
    MAP,

    /*
     * Messenger
     */
    TELEGRAM,
    SLACK,
    ROCKET_CHAT,
    HANGOUTS,
    DISCORD,

    /*
     * Dialogue automaton
     */
    DIALOGUE,

    /*
     * Social Networks
     */
    TWITTER,

    /*
     * Services
     */
    OPENAPI,
    EMAIL,
    RSS,
    SQL,

    /*
     * Timer
     */
    ROUTINE
}
