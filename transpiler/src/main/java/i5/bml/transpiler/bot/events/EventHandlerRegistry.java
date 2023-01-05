package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.dialogue.DialogueHandler;
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
                    var eventHandlers = m.getAnnotationsByType(MessageEventHandlerMethod.class);
                    for (var eventHandler : eventHandlers) {
                        messageEventHandler.put(eventHandler.messageEventType(), m);
                    }
                });
    }

    public static void dispatchEventHandler(Event event) {
        switch (event.eventSource()) {
            case SLACK, TELEGRAM -> {
                var messageEvent = (MessageEvent) event;

                // We send the message to the desired NLU to infer intent and entity/entities
                DialogueHandler.handleMessageEvent(messageEvent);

                try {
                    messageEventHandler.get(messageEvent.messageEventType()).invoke(null, new MessageEventContext(messageEvent));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("No message handler registered for message event type %s".formatted(messageEvent.messageEventType()), e);
                }
            }
        }
    }
}
