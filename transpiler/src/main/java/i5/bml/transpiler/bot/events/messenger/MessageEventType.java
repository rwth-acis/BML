package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.utils.Utils;

public enum MessageEventType {

    USER_STARTED_CHAT,

    USER_SENT_MESSAGE,

    USER_JOINED_CHAT,

    USER_LEFT_CHAT,

    BOT_ADDED,

    BOT_REMOVED,

    BOT_COMMAND;

    @Override
    public String toString() {
        return Utils.pascalCaseToSnakeCase(name());
    }
}
