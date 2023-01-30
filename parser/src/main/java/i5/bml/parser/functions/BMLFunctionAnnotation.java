package i5.bml.parser.functions;

import java.lang.annotation.*;

/**
 * The BMLFunctionAnnotation annotation is used to annotate BML functions.
 * <p>
 * We collect functions annotated with this annotation in {@link FunctionRegistry}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BMLFunctionAnnotation {
    /**
     * Returns the scope of the function.
     *
     * @return the scope of the function.
     */
    BMLFunctionScope scope();

    /**
     * Returns the name of the function.
     *
     * @return the name of the function.
     */
    String name();
}
