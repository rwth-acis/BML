package i5.bml.transpiler.bot;

import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventHandlerRegistry;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventHandler;
import i5.bml.transpiler.bot.events.routines.RoutineEventHandler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Bot {

    private final PriorityBlockingQueue<Event> eventQueue = new PriorityBlockingQueue<>(100,
            Comparator.comparingLong(Event::getArrivalTime));

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
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(BotConfig.ROUTINE_COUNT);
        RoutineEventHandler.registerEventHandler(scheduler);
    }

    public void run() {
        while (true) {
            try {
                var event = eventQueue.take();
                System.out.println("CURR EVENT: " + event);

                if (event instanceof MessageEvent
                        && ((MessageEvent) event).getSession() != null) {
                    var messageEvent = (MessageEvent) event;
                    var currentChatId = messageEvent.getSession().getChatId();
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
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                        return null;
                    });

                    previousEventCompletableFuture.put(currentChatId, newCompletableFuture);
                } else {
                    threadPool.execute(() -> EventHandlerRegistry.dispatchEventHandler(event));
                }
            } catch (InterruptedException ignore) {
            }
        }
    }
}
