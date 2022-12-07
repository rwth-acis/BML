package i5.bml.transpiler.bot;

public class Session {

    // TODO: Change to automaton state
    private Object state;

    private final Object chatId;

    public Session(Object chatId) {
        this.chatId = chatId;
        // Construct automaton
    }

    public Object getChatId() {
        return chatId;
    }

    @Override
    public String toString() {
        return "Session{state=%s, chatId=%s}".formatted(state, chatId);
    }
}
