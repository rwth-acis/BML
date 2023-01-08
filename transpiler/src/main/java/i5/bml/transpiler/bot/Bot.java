package i5.bml.transpiler.bot;

import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventHandlerRegistry;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventHandler;
import i5.bml.transpiler.bot.events.routines.RoutineEventHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Bot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);

    private final PriorityBlockingQueue<Event> eventQueue = new PriorityBlockingQueue<>(100,
            Comparator.comparingLong(Event::arrivalTime));

    /**
     * This pool offers threads for the components that are external event resources.
     * It is also used to submit event handlers for incoming events. Hence, incoming events
     * are dealt with in a "multi-threaded" manner.
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final Map<Object, CompletableFuture<Void>> previousEventCompletableFuture = new HashMap<>();

    public Bot() {
        // Register message event handlers
        EventHandlerRegistry.registerEventHandler(MessageEventHandler.class);

        // Components (external event sources)
        ComponentRegistry.initComponents(threadPool, eventQueue);

        // Routines (internal event sources)
        RoutineEventHandler.registerEventHandler(new ScheduledThreadPoolExecutor(1));
    }

    public void run() {
        //noinspection InfiniteLoopStatement -> The infinite loop is desired
        while (true) {
            try {
                var event = eventQueue.take();

                if (event instanceof MessageEvent messageEvent && ((MessageEvent) event).session() != null) {
                    var currentChatId = messageEvent.session().chatId();
                    var prevFuture = previousEventCompletableFuture.get(currentChatId);
                    CompletableFuture<Void> newCompletableFuture;
                    if (prevFuture != null) {
                        newCompletableFuture = prevFuture.thenRunAsync(() ->
                                EventHandlerRegistry.dispatchEventHandler(event), threadPool);
                    } else {
                        newCompletableFuture = CompletableFuture.runAsync(() ->
                                EventHandlerRegistry.dispatchEventHandler(event), threadPool);
                    }

                    // Add exception handling
                    newCompletableFuture.exceptionally(e -> {
                        LOGGER.error("An exception occurred while dispatching handler for event {}:\n{}", event, ExceptionUtils.getRootCause(e).getMessage());
                        return null;
                    });

                    previousEventCompletableFuture.put(currentChatId, newCompletableFuture);
                } else {
                    threadPool.execute(() -> EventHandlerRegistry.dispatchEventHandler(event));
                }
            } catch (InterruptedException e) {
                LOGGER.error("Execution of event main loop was interrupted: {}", ExceptionUtils.getRootCause(e).getMessage());
            }
        }
    }
}
