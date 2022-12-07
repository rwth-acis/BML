package i5.bml.transpiler.bot.events.messenger;

public class MessageEventContext {

    private final MessageEvent event;

    public MessageEventContext(MessageEvent event) {
        this.event = event;
    }

    public MessageEvent getEvent() {
        return event;
    }

    public String getIntent() {
        return "";
    }

    public String getEntity() {
        return "";
    }
}
