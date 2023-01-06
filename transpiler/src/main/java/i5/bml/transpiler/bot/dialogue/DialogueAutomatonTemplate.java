package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.*;

public class DialogueAutomatonTemplate implements DialogueAutomaton {

    private final List<State> states = new ArrayList<>();

    private State currentState;

    private State defaultState;

    public void initTransitions() {
        currentState = defaultState;
        states.add(defaultState);
    }

    @Override
    public void step(MessageEventContext ctx) {
        var newState = currentState.nextState(ctx.intent());

        // No state matches, check if wildcard is declared
        currentState = Objects.requireNonNullElseGet(newState, () -> {
            // Go to default state if no wildcard state is declared
            return Objects.requireNonNullElse(currentState.nextState("_"), defaultState);
        });

        currentState.action(ctx);

        // Check whether "new" state is fallthrough, if so, fall through
        var fallthroughState = currentState.transitions.get("");
        if (fallthroughState != null) {
            jumpTo(fallthroughState, ctx);
        }
    }

    @Override
    public void jumpTo(State state, MessageEventContext ctx) {
        currentState = state;
        currentState.action(ctx);
    }

    @Override
    public void jumpToWithoutAction(State state) {
        currentState = state;
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
