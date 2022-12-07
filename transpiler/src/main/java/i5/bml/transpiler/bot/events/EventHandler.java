package i5.bml.transpiler.bot.events;

import i5.bml.transpiler.bot.events.messenger.MessageEventType;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(EventHandler.List.class)
public @interface EventHandler {

    MessageEventType messageEventType();

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        EventHandler[] value();
    }
}
