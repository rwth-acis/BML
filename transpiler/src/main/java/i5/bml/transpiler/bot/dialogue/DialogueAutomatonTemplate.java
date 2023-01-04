package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.*;

public class DialogueAutomatonTemplate implements DialogueAutomaton {

    private final Set<State> states = new HashSet<>();

    private State currentState = new State(ctx -> {});

    public void initTransitions() {
    }

    @Override
    public void step(MessageEventContext context) {
        currentState = currentState.nextState(context.intent());
        currentState.action(context);
    }

    @Override
    public void jumpTo(State state, MessageEventContext context) {
        currentState = state;
        currentState.action(context);
    }
}
