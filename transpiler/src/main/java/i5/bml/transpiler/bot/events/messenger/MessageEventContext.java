package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.Context;

public record MessageEventContext(MessageEvent event) implements Context {

    @Override
    public String intent() {
        return event.intent();
    }

    @Override
    public String entity() {
        return event.entity();
    }

    public User user() {
        return event.user();
    }
}
