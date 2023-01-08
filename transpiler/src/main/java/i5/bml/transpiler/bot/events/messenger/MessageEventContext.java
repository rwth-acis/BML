package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.Context;
import i5.bml.transpiler.bot.threads.User;

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
