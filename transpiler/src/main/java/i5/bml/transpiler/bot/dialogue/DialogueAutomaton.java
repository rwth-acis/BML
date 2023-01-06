package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

public interface DialogueAutomaton {

    void step(MessageEventContext ctx);

    void jumpTo(State state, MessageEventContext ctx);

    void jumpToWithoutAction(State state);

    State defaultState();
}
