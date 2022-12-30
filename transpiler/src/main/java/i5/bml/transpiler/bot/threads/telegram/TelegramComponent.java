package i5.bml.transpiler.bot.threads.telegram;

import i5.bml.transpiler.bot.threads.Session;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.telegram.TelegramUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TelegramComponent extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramComponent.class);

    private final PriorityBlockingQueue<Event> eventQueue;

    private final String botName;

    private final String botToken;

    private final Map<Long, Session> activeSessions = new HashMap<>();

    public TelegramComponent(PriorityBlockingQueue<Event> eventQueue, String botName, String botToken) {
        this.eventQueue = eventQueue;
        this.botName = botName;
        this.botToken = botToken;

        // We check for inactive sessions every day
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            var keys = new HashSet<>(activeSessions.keySet());
            for (Long chatId : keys) {
                var chatAction = new SendChatAction();
                chatAction.setChatId(chatId);
                chatAction.setAction(ActionType.TYPING);
                try {
                    // We remove a session should the request be unsuccessful
                    if (!sendApiMethod(chatAction)) {
                        activeSessions.remove(chatId);
                    }
                    // We sleep 33 ms, since only 30 sendChatActions / second are allowed
                    Thread.sleep(33);
                } catch (TelegramApiException | InterruptedException ignore) {}
            }
        }, 1, 1, TimeUnit.DAYS);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            MessageEvent telegramEvent = new MessageEvent(EventSource.TELEGRAM, update.getMessage().getDate());
            System.out.println(update);
            if (filterUpdates(telegramEvent, update)) {
                eventQueue.put(telegramEvent);
            }
        } catch (Exception e) {
            LOGGER.error("Error while receiving update from Telegram server", e);
        }
    }

    private boolean filterUpdates(MessageEvent telegramEvent, Update update) {
        if (update.getMyChatMember() != null && update.getMyChatMember().getChat().isGroupChat()) {
            long chatId = update.getMyChatMember().getChat().getId();

            if (update.getMyChatMember().getNewChatMember().getStatus().equals("left")) {
                telegramEvent.setMessageEventType(MessageEventType.BOT_REMOVED);

                activeSessions.remove(chatId);
            } else if (update.getMyChatMember().getNewChatMember().getStatus().equals("member")) {
                telegramEvent.setMessageEventType(MessageEventType.BOT_ADDED);

                activeSessions.put(chatId, new Session(chatId));
            } else {
                // myChatMember can be one of
                // [ChatMemberOwner, ChatMemberAdministrator, ChatMemberMember, ChatMemberRestricted, ChatMemberLeft, ChatMemberBanned]
                // see https://core.telegram.org/bots/api#chatmember
                // We only want ChatMemberMember (sent whenever a bot becomes member of a group)
                // and ChatMemberLeft (sent whenever bot is removed from group)
                return false;
            }

            telegramEvent.setUsername(update.getMyChatMember().getFrom().getUserName());
            telegramEvent.setUser(new TelegramUser(this, chatId));
        } else if (update.getMessage() != null) {
            long chatId = update.getMessage().getChatId();

            if (update.getMessage().getLeftChatMember() != null
                    && !update.getMessage().getLeftChatMember().getUserName().equals(botName)) {
                telegramEvent.setMessageEventType(MessageEventType.USER_LEFT_CHAT);
                telegramEvent.setUsername(update.getMessage().getLeftChatMember().getUserName());
            } else if (!update.getMessage().getNewChatMembers().isEmpty()) {
                var newChatMembers = update.getMessage().getNewChatMembers();
                var botWasAdded = newChatMembers.stream().anyMatch(u -> u.getUserName().equals(botName));
                if (!botWasAdded) {
                    telegramEvent.setMessageEventType(MessageEventType.USER_JOINED_CHAT);
                    telegramEvent.setUsername(newChatMembers.get(0).getUserName());
                } else {
                    // There is an explicit message with "newChatMembers", but we use the service message about change
                    // of member status to find out whether a bot was added to a group
                    return false;
                }
            } else if (update.getMessage().getText() != null) {
                if (update.getMessage().getEntities() != null) {
                    var entity = update.getMessage().getEntities().get(0);
                    if (entity.getText().equals("/start")) {
                        telegramEvent.setMessageEventType(MessageEventType.USER_STARTED_CHAT);

                        // Create Session
                        // This means that using "/start" in a chat, RESETS the current conversation status
                        var session = new Session(chatId);
                        activeSessions.put(chatId, session);
                    } else if (entity.getType().equals("bot_command")) {
                        telegramEvent.setMessageEventType(MessageEventType.BOT_COMMAND);
                        telegramEvent.setText(update.getMessage().getEntities().get(0).getText());
                        var args = update.getMessage().getText().split(" ");
                        telegramEvent.setCommandArguments(Arrays.stream(args).toList().subList(1, args.length));
                    } else {
                        return false;
                    }
                } else if (!update.getMessage().getText().isEmpty()) {
                    telegramEvent.setMessageEventType(MessageEventType.USER_SENT_MESSAGE);
                    telegramEvent.setText(update.getMessage().getText());
                } else {
                    return false;
                }

                telegramEvent.setSession(activeSessions.get(chatId));
                telegramEvent.setUsername(update.getMessage().getFrom().getUserName());
            } else {
                return false;
            }

            telegramEvent.setUser(new TelegramUser(this, chatId));
        } else {
            return false;
        }

        return true;
    }



    @Override
    public String toString() {
        return "TelegramComponent{username='%s', token='%s', activeSessions=%s}".formatted(botName, botToken, activeSessions);
    }
}
