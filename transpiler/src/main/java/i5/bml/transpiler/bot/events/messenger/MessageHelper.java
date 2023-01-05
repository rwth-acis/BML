package i5.bml.transpiler.bot.events.messenger;

import com.slack.api.methods.SlackApiException;
import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;
import i5.bml.transpiler.bot.events.messenger.telegram.TelegramUser;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class MessageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);

    public static void replyToMessenger(User user, String msg) {
        if (user instanceof TelegramUser telegramUser) {
            sendTelegramMessage(telegramUser.telegramComponent(), telegramUser.chatId(), msg);
        } else if (user instanceof SlackUser slackUser) {
            sendSlackMessage(slackUser.slackClient(), slackUser.botToken(), slackUser.channelId(), msg);
        } else {
            LOGGER.error("Unknown user type {}", user.getClass());
        }
    }

    public static void replyToMessenger(MessageEventContext context, String msg) {
        replyToMessenger(context.event().user(), msg);
    }

    private static void sendTelegramMessage(TelegramComponent telegramComponent, Long chatId, String msg) {
        try {
            var send = new SendMessage();
            send.setChatId(chatId);
            send.setText(msg);
            telegramComponent.execute(send);
        } catch (TelegramApiException e) {
            LOGGER.error("An error occurred while sending the msg '{}' to the chat with id {} using the telegram bot {}:\n{}",
                    msg, chatId, telegramComponent.getBotUsername(), e.getMessage());
        }
    }

    private static void sendSlackMessage(SocketModeClient slackClient, String botToken, String channelId, String msg) {
        try {
            slackClient.getSlack().methods().chatPostMessage(r -> r.token(botToken).channel(channelId).text(msg));
        } catch (IOException | SlackApiException e) {
            LOGGER.error("An error occurred while sending the msg '{}' to the chat with id {} using the slack bot:\n{}",
                    msg, channelId, e.getMessage());
        }
    }
}
