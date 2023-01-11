package i5.bml.parser.functions;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BMLFunctionAnnotation {

    BMLFunctionScope scope();

    String name();
}
