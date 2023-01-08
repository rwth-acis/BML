package i5.bml.transpiler.bot.events.routines;

import i5.bml.transpiler.bot.events.RoutineEventHandlerMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RoutineEventHandler {

    public static void registerEventHandler(ScheduledThreadPoolExecutor scheduler) {
        Arrays.stream(RoutineEventHandler.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(RoutineEventHandlerMethod.class))
                .forEach(m -> {
                    var annotation = m.getAnnotation(RoutineEventHandlerMethod.class);
                    scheduler.setCorePoolSize(scheduler.getCorePoolSize() + 1);
                    scheduler.scheduleAtFixedRate(() -> {
                        try {
                            m.invoke(null, (Object) null);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }, 0, annotation.period(), annotation.timeUnit());
                });
    }
}
