package i5.bml.transpiler.bot.threads;

import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.DialogueFactory;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;

import java.util.List;

public class Session {

    private final List<DialogueAutomaton> dialogues;

    private final Object chatId;

    public Session(Object chatId, MessageEventType messageEventType) {
        this.chatId = chatId;
        dialogues = DialogueFactory.createDialogue(messageEventType);
    }

    public List<DialogueAutomaton> dialogues() {
        return dialogues;
    }

    public Object chatId() {
        return chatId;
    }

    @Override
    public String toString() {
        return "Session{dialogues=%s, chatId=%s}".formatted(dialogues, chatId);
    }
}
