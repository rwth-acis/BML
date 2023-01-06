package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.*;

public class DialogueAutomatonTemplate implements DialogueAutomaton {

    private final Set<State> states = new HashSet<>();

    private State currentState = new State(ctx -> {});

    private State defaultState;

    public void initTransitions() {
        currentState = defaultState;
    }

    @Override
    public void step(MessageEventContext ctx) {
        currentState = currentState.nextState(ctx.intent());
        currentState.action(ctx);

        // Check whether the _only_ outgoing state (except default state) is fallthrough
        var fallthroughState = currentState.transitions.get("");
        if (currentState.transitions.size() <= 2 && fallthroughState != null) {
            jumpTo(fallthroughState, ctx);
        }
    }

    @Override
    public void jumpTo(State state, MessageEventContext ctx) {
        currentState = state;
        currentState.action(ctx);
    }

    @Override
    public State defaultState() {
        return defaultState;
    }

    @Override
    public String toString() {
        return "%s{currentState=%s}".formatted(getClass().getSimpleName(), currentState);
    }
}
