package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.Context;

public class MessageEventContext implements Context {

    private final MessageEvent event;

    public MessageEventContext(MessageEvent event) {
        this.event = event;
    }


    @Override
    public MessageEvent event() {
        return event;
    }

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
