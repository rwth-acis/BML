package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.threads.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);

    private MessageHelper() {}

    public static void replyToMessenger(User user, String msg) {

    }

    public static void replyToMessenger(MessageEventContext context, String msg) {
        replyToMessenger(context.event().user(), msg);
    }

    public static void replyToMessenger(User user, String msg, List<List<String>> buttonRows) {

    }

    public static void replyToMessenger(MessageEventContext context, String msg, List<List<String>> buttonRows) {
        replyToMessenger(context.event().user(), msg, buttonRows);
    }
}
