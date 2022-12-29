package i5.bml.transpiler.bot.components;

import i5.bml.transpiler.bot.events.Event;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

public class ComponentRegistry {

    public static void initComponents(ExecutorService threadPool, PriorityBlockingQueue<Event> eventQueue) {
        Arrays.stream(ComponentRegistry.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ComponentInitializer.class))
                .forEach(m -> {
                    try {
                        m.invoke(null, threadPool, eventQueue);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
