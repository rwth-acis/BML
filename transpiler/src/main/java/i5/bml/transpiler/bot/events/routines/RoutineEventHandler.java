package i5.bml.transpiler.bot.events.routines;

import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.events.RoutineEventHandlerMethod;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RoutineEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentRegistry.class);

    private RoutineEventHandler() {}

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
                            LOGGER.error("Invoking routine {} failed", m.getDeclaringClass().getSimpleName(), ExceptionUtils.getRootCause(e));
                        }
                    }, 0, annotation.period(), annotation.timeUnit());
                });
    }
}
