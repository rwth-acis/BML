package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueAutomatonTemplate implements DialogueAutomaton {

    private final Map<String, State> namedStates = new HashMap<>();

    private State currentState;

    private List<Integer> sinkStates;

    private int defaultState;

    public void init() {

    }

    @Override
    public void step(MessageEventContext context) {
        currentState = currentState.getNextState(context.getIntent());
        currentState.action(context);
    }

    public void jumpTo(State state) {
        currentState = state;
    }
}