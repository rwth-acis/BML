package i5.bml.transpiler.bot.components;

import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.threads.telegram.TelegramBotThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.*;

public class ComponentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentRegistry.class);

    public static void initComponents(ExecutorService threadPool, PriorityBlockingQueue<Event> eventQueue) {
        var futures = Arrays.stream(ComponentRegistry.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ComponentInitializer.class))
                .map(m -> CompletableFuture.runAsync(() -> {
                    try {
                        m.invoke(null, threadPool, eventQueue);
                    } catch (IllegalAccessException | InvocationTargetException ignore) {}
                }, threadPool).exceptionally(e -> {
                    throw new IllegalStateException(e);
                }))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures).get();
            LOGGER.info("Component initialization done!");
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Component initialization failed", e);
        }
    }
}
