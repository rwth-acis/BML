package i5.bml.parser.types.dialogue;

import org.antlr.symtab.Type;

public class BMLState {

    private String intent;

    private Object action;

    private Type actionType;


    public String getIntent() {
        return intent;
    }

    public Object getAction() {
        return action;
    }

    public Type getActionType() {
        return actionType;
    }
}
