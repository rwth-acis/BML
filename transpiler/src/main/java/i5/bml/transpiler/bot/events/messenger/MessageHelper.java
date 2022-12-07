package i5.bml.transpiler.bot.events.messenger;

import com.slack.api.methods.SlackApiException;
import i5.bml.transpiler.bot.ComponentRegistry;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;
import i5.bml.transpiler.bot.events.messenger.telegram.TelegramUser;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class MessageHelper {

    public static void replyToMessenger(User user, String msg) {
        if (user instanceof TelegramUser telegramUser) {
            sendTelegramMessage(telegramUser.telegramComponent(), telegramUser.chatId(), msg);
        } else if (user instanceof SlackUser slackUser) {
            sendSlackMessage(slackUser.botToken(), slackUser.channelId(), msg);
        } else {
            // TODO: Throw exception
        }
    }
    public static void replyToMessenger(MessageEventContext context, String msg) {
        if (context.getEvent().getUser() instanceof TelegramUser telegramUser) {
            sendTelegramMessage(telegramUser.telegramComponent(), telegramUser.chatId(), msg);
        } else if (context.getEvent().getUser() instanceof SlackUser slackUser) {
            sendSlackMessage(slackUser.botToken(), slackUser.channelId(), msg);
        } else {
            // TODO: Throw exception
        }
    }

    private static void sendTelegramMessage(TelegramComponent telegramComponent, Long chatId, String msg) {
        try {
            var send = new SendMessage();
            send.setChatId(chatId);
            send.setText(msg);
            telegramComponent.execute(send);
        } catch (TelegramApiException e) {
            // TODO: Throw new exception
            e.printStackTrace();
        }
    }

    private static void sendSlackMessage(String botToken, String channelId, String msg) {
        try {
            ComponentRegistry.getSlackComponent()
                    .getSlack()
                    .methods()
                    .chatPostMessage(r -> r.token(botToken)
                            .channel(channelId)
                            .text(msg)
                    );
        } catch (IOException | SlackApiException e) {
            // TODO: Throw new exception
            e.printStackTrace();
        }
    }
}
