package i5.bml.transpiler.bot.threads.telegram;

import i5.bml.transpiler.bot.events.Event;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

public class TelegramThread implements Runnable {

    private final LinkedBlockingDeque<Event> eventQueue;

    private final String username;

    private final String token;

    public TelegramThread(LinkedBlockingDeque<Event> eventQueue, String username, String token) {
        this.eventQueue = eventQueue;
        this.username = username;
        this.token = token;
    }

    @Override
    public void run() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            var telegramBot = new TelegramComponent(eventQueue, username, token);
            telegramBotsApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            // TODO: Proper error handling
            e.printStackTrace();
        }
    }
}
