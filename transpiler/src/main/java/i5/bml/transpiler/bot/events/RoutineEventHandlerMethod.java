package i5.bml.transpiler.bot.events;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoutineEventHandlerMethod {

    long period();

    TimeUnit timeUnit();
}
