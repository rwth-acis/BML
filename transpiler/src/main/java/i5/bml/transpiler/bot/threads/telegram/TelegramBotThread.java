package i5.bml.transpiler.bot.threads.telegram;

import i5.bml.transpiler.bot.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.PriorityBlockingQueue;

public class TelegramBotThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotThread.class);

    private final PriorityBlockingQueue<Event> eventQueue;

    private final String botName;

    private final String botToken;

    public TelegramBotThread(PriorityBlockingQueue<Event> eventQueue, String botName, String botToken) {
        this.eventQueue = eventQueue;
        this.botName = botName;
        this.botToken = botToken;
    }

    @Override
    public void run() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            var telegramBot = new TelegramComponent(eventQueue, botName, botToken);
            telegramBotsApi.registerBot(telegramBot);
            LOGGER.info("Successfully initialized Telegram bot with name {}", botName);
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to initialize Telegram: {}", e.getMessage());
            // We have to rethrow to make sure that the component initialization receives the failure signal
            throw new RuntimeException(e);
        }
    }
}
