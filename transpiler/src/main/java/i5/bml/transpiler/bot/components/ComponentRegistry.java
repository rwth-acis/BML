package i5.bml.transpiler.bot.components;

import i5.bml.transpiler.bot.events.Event;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.*;

public class ComponentRegistry {

    public static void initComponents(ExecutorService threadPool, PriorityBlockingQueue<Event> eventQueue) {
        var futures = Arrays.stream(ComponentRegistry.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ComponentInitializer.class))
                .map(m -> CompletableFuture.runAsync(() -> {
                    try {
                        m.invoke(null, threadPool, eventQueue);
                    } catch (IllegalAccessException | InvocationTargetException ignore) {}
                }, threadPool).exceptionally(e -> {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return null;
                }))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures).get();
            System.out.println("Component initialization done!");
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Component initialization failed", e);
        }
    }
}
