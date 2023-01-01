package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

public interface DialogueAutomaton {

    void step(MessageEventContext context);

    void jumpTo(State state, MessageEventContext context);
}
