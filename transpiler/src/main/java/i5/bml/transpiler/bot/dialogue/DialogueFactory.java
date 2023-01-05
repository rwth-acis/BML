package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DialogueFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueFactory.class);

    public static List<DialogueAutomaton> createDialogue(MessageEventType messageEventType) {
        switch (messageEventType) {

            default: LOGGER.error("No dialogue registered for message event {}", messageEventType);
        }

        return null;
    }
}
