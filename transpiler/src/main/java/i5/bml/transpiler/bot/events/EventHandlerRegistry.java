package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventHandlerRegistry {

    private static final Map<MessageEventType, Method> messageEventHandler = new HashMap<>();

    private EventHandlerRegistry() {}

    public static void registerEventHandler(Class<?> eventHandlerClazz) {
        Arrays.stream(eventHandlerClazz.getDeclaredMethods())
                .forEach(m -> {
                    var eventHandlers = m.getAnnotationsByType(EventHandler.class);
                    for (var eventHandler : eventHandlers) {
                        messageEventHandler.put(eventHandler.messageEventType(), m);
                    }
                });
    }

    public static void dispatchEventHandler(Event event) {
        switch (event.getEventSource()) {
            case TELEGRAM, SLACK -> {
                // TODO: Invoke rasa with text from event and set entities and intents

                MessageEventContext messageEventContext = new MessageEventContext((MessageEvent) event);

                try {
                    messageEventHandler.get(((MessageEvent) event).getMessageEventType()).invoke(null, messageEventContext);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // TODO: Proper error handling
                    e.printStackTrace();
                }
            }
        }
    }
}
