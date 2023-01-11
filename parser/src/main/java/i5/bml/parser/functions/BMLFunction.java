package i5.bml.parser.functions;

import org.antlr.symtab.Scope;

public interface BMLFunction {

    default String getName() {
        return this.getClass().getAnnotation(BMLFunctionAnnotation.class).name();
    }

    void defineFunction(Scope scope);
}
