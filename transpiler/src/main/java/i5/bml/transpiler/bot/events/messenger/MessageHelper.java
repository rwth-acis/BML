package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.threads.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);

    public static void replyToMessenger(User user, String msg) {

    }

    public static void replyToMessenger(MessageEventContext context, String msg) {
        replyToMessenger(context.event().user(), msg);
    }
}
