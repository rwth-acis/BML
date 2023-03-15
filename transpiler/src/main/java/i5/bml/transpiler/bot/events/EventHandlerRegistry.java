package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class EventHandlerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandlerRegistry.class);

    private static final Map<MessageEventType, Method> messageEventHandler = new EnumMap<>(MessageEventType.class);

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
        EventSource eventSource = event.eventSource();
        if (eventSource == null) {
            LOGGER.error("Event {} unexpectedly had null as event source. Not executing any event handlers.", event);
            return;
        }

        if (eventSource == EventSource.SLACK || eventSource == EventSource.TELEGRAM) {
            var messageEvent = (MessageEvent) event;

            var handler = messageEventHandler.get(messageEvent.messageEventType());
            if (handler == null) {
                LOGGER.warn("No handler registered for message event {}", messageEvent.messageEventType());
            } else {
                try {
                    handler.invoke(null, new MessageEventContext(messageEvent));
                } catch (Exception e) {
                    LOGGER.error("Execution of handler for message event {} failed: {}", messageEvent.messageEventType(), e.getCause().getMessage());
                    LOGGER.debug("Stacktrace:", e);
                }
            }
        }
    }
}
