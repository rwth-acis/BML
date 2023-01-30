package i5.bml.parser.functions;

/**
 * Represents the scope of a {@link BMLFunction}.
 */
public enum BMLFunctionScope {

    /**
     * The function is global and can be called from any part of the code.
     */
    GLOBAL,

    /**
     * The function is defined in the context of a dialogue and can only be called within a dialogue.
     */
    DIALOGUE
}
