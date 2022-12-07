package i5.bml.parser.types;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BMLComponentParameter {

    String name();

    BuiltinType expectedBMLType();

    boolean isRequired();
}
