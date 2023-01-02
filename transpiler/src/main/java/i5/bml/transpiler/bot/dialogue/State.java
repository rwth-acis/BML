package i5.bml.transpiler.bot.dialogue;

import i5.bml.transpiler.bot.events.messenger.MessageEventContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class State {

    protected final Map<String, State> transitions = new HashMap<>();

    private Consumer<MessageEventContext> action;

    public State() {}

    public State(Consumer<MessageEventContext> action) {
        this.action = action;
    }

    public void action(MessageEventContext context) {
        action.accept(context);
    }

    public State nextState(String intent) {
        return transitions.get(intent);
    }

    public void addTransition(String intent, State target) {
        transitions.put(intent, target);
    }

    public void setAction(Consumer<MessageEventContext> action) {
        this.action = action;
    }
}
