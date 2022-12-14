package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DialogueFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueFactory.class);

    private DialogueFactory() {}

    public static List<DialogueAutomaton> createDialogue(MessageEventType messageEventType) {
        return switch (messageEventType) {
            default -> {
                LOGGER.error("No dialogue registered for message event {}", messageEventType);
                yield List.of();
            }
        };
    }
}
