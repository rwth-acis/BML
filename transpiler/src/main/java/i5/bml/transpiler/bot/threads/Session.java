package i5.bml.transpiler.bot.threads;

import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;

public class Session {

    // TODO: Change to automaton state
    private DialogueAutomaton dialogue;

    private final Object chatId;

    public Session(Object chatId) {
        this.chatId = chatId;
        // Construct automaton
    }

    public DialogueAutomaton getDialogue() {
        return dialogue;
    }

    public Object getChatId() {
        return chatId;
    }

    @Override
    public String toString() {
        return "Session{dialogue=%s, chatId=%s}".formatted(dialogue, chatId);
    }
}
