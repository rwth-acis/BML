package i5.bml.transpiler.bot;

import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventHandlerRegistry;
import i5.bml.transpiler.bot.events.messenger.MessageEventHandler;
import i5.bml.transpiler.bot.events.routines.RoutineEventHandler;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.bot.threads.telegram.TelegramThread;

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

    // TODO: Number determined by @Routine in code
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Map<Object, CompletableFuture<Void>> previousEventCompletableFuture = new HashMap<>();

    public Bot() {
    }

    private void init(String botToken) {
        // Register event handlers
        EventHandlerRegistry.registerEventHandler(MessageEventHandler.class);

        // External event sources
        try {
            threadPool.submit(new TelegramThread(eventQueue, "TestBMLBot", botToken)).get();
            ComponentRegistry.setSlackComponent(threadPool.submit(new SlackBotThread(eventQueue)).get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Routines (internal event sources)
        RoutineEventHandler routineEventHandler = new RoutineEventHandler();
        //scheduler.scheduleAtFixedRate(() -> routineEventHandler.reportPetStatus(null), 0, 1, TimeUnit.MINUTES);
    }

    public void run(String botToken) {
        init(botToken);

        while (true) {
            try {
                var event = eventQueue.take();
                System.out.println("CURR EVENT: " + event);

                if (event instanceof MessageEvent messengerEvent
                        && ((MessageEvent) event).getSession() != null) {
                    var currentChatId = messengerEvent.getSession().getChatId();
                    var prevFuture = previousEventCompletableFuture.get(currentChatId);
                    CompletableFuture<Void> newCompletableFuture;
                    if (prevFuture != null) {
                        newCompletableFuture = prevFuture.thenRunAsync(() ->
                                EventHandlerRegistry.dispatchEventHandler(event), threadPool);
                    } else {
                        newCompletableFuture = CompletableFuture.runAsync(() ->
                                EventHandlerRegistry.dispatchEventHandler(event), threadPool);
                    }

                    previousEventCompletableFuture.put(currentChatId, newCompletableFuture);
                } else {
                    threadPool.execute(() -> EventHandlerRegistry.dispatchEventHandler(event));
                }
            } catch (InterruptedException ignore) {
            }
        }
    }
}
