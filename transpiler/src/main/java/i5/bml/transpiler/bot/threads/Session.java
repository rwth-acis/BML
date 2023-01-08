package i5.bml.transpiler.bot.threads;

import i5.bml.transpiler.bot.events.messenger.MessageEventType;

public class Session {

    private final Object chatId;

    public Session(Object chatId, MessageEventType messageEventType) {
        this.chatId = chatId;
    }

    public Object chatId() {
        return chatId;
    }
}
