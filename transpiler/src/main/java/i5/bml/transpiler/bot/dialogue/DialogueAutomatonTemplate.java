package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.config.BotConfig;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.*;

public class DialogueAutomatonTemplate implements DialogueAutomaton {

    private final List<State> states = new ArrayList<>();

    private final Map<String, State> namedStates = new HashMap<>();

    private State currentState;

    private State defaultState;

    private String fallbackIntent;

    public DialogueAutomatonTemplate() {
        initTransitions();
    }

    public void initTransitions() {
        currentState = defaultState;
        states.add(defaultState);
    }

    @Override
    public void step(MessageEventContext ctx) {
        var newState = currentState.nextState(ctx.intent());

        // No state matches, use fallback
        currentState = Objects.requireNonNullElseGet(newState, () -> currentState.nextState(fallbackIntent));

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
    public State getStateByName(String stateName) {
        return namedStates.get(stateName);
    }

    @Override
    public String toString() {
        return "%s{currentState=%s}".formatted(getClass().getSimpleName(), currentState);
    }
}
