package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.Context;

public class MessageEventContext implements Context {

    private final MessageEvent event;

    public MessageEventContext(MessageEvent event) {
        this.event = event;
    }


    @Override
    public MessageEvent getEvent() {
        return event;
    }

    @Override
    public String getIntent() {
        return event.getIntent();
    }

    @Override
    public String getEntity() {
        return event.getEntity();
    }

    public User getUser() {
        return event.getUser();
    }
}
