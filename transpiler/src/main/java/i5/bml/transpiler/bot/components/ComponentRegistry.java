package i5.bml.transpiler.bot.components;

import i5.bml.transpiler.bot.events.Event;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

public class ComponentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentRegistry.class);

    private ComponentRegistry() {}

    public static void initComponents(ExecutorService threadPool, PriorityBlockingQueue<Event> eventQueue) {
        var futures = Arrays.stream(ComponentRegistry.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ComponentInitializer.class))
                .map(m -> {
                    try {
                        return (CompletableFuture<?>) m.invoke(null, threadPool, eventQueue);
                    } catch (Exception e) {
                        LOGGER.error("Component initialization failed for {}", m.getDeclaringClass().getSimpleName(), ExceptionUtils.getRootCause(e));
                        return CompletableFuture.failedFuture(e);
                    }
                })
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(futures).get();
            LOGGER.info("Component initialization done!");
        } catch (InterruptedException e) {
            LOGGER.error("Component initialization was interrupted", ExceptionUtils.getRootCause(e));
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Component initialization failed", ExceptionUtils.getRootCause(e));
        }
    }
}
