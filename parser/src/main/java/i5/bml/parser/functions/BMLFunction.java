package i5.bml.parser.functions;

import org.antlr.symtab.Scope;

/**
 * The BMLFunction interface defines a function in the BML language.
 */
public interface BMLFunction {

    /**
     * Returns the name of the function.
     *
     * @return the name of the function.
     */
    default String getName() {
        return this.getClass().getAnnotation(BMLFunctionAnnotation.class).name();
    }

    /**
     * Defines the function in the specified {@code scope}.
     *
     * @param scope the scope in which to define the function.
     */
    void defineFunction(Scope scope);
}
