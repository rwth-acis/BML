package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.dialogue.DialogueHandler;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventHandlerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandlerRegistry.class);

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

                var handler = messageEventHandler.get(messageEvent.messageEventType());
                if (handler == null) {
                    LOGGER.warn("No handler registered for message event {}", messageEvent.messageEventType());
                } else {
                    try {
                        handler.invoke(null, new MessageEventContext(messageEvent));
                    } catch (Exception e) {
                        LOGGER.error("Execution of handler for message event {} failed", messageEvent.messageEventType(), e);
                    }
                }
            }
        }
    }
}
