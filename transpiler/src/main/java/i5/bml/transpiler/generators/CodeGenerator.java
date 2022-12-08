package i5.bml.transpiler.generators;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CodeGenerator {

    Class<?> typeClass();
}
