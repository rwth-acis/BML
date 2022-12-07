package i5.bml.parser.types;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BMLType {
    BuiltinType name();

    boolean isComplex();
}
