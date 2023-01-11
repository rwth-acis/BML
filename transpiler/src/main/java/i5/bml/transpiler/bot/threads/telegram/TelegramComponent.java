package i5.bml.transpiler.bot.threads.telegram;

import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.threads.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

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
    public void onUpdateReceived(Update update) {
        try {
            MessageEvent telegramEvent = new MessageEvent(EventSource.TELEGRAM, update.getMessage().getDate());
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
                telegramEvent.messageEventType(MessageEventType.BOT_REMOVED);
                activeSessions.remove(chatId);
            } else if (update.getMyChatMember().getNewChatMember().getStatus().equals("member")) {
                telegramEvent.messageEventType(MessageEventType.BOT_ADDED);
                activeSessions.put(chatId, new Session(chatId, MessageEventType.BOT_ADDED));
            } else {
                // myChatMember can be one of
                // [ChatMemberOwner, ChatMemberAdministrator, ChatMemberMember, ChatMemberRestricted, ChatMemberLeft, ChatMemberBanned]
                // see https://core.telegram.org/bots/api#chatmember
                // We only want ChatMemberMember (sent whenever a bot becomes member of a group)
                // and ChatMemberLeft (sent whenever bot is removed from group)
                return false;
            }

            telegramEvent.username(update.getMyChatMember().getFrom().getUserName());
            telegramEvent.user(new TelegramUser(this, chatId));
        } else if (update.getMessage() != null) {
            long chatId = update.getMessage().getChatId();

            if (update.getMessage().getLeftChatMember() != null
                    && !update.getMessage().getLeftChatMember().getUserName().equals(botName)) {
                telegramEvent.messageEventType(MessageEventType.USER_LEFT_CHAT);
                telegramEvent.username(update.getMessage().getLeftChatMember().getUserName());
            } else if (!update.getMessage().getNewChatMembers().isEmpty()) {
                var newChatMembers = update.getMessage().getNewChatMembers();
                var botWasAdded = newChatMembers.stream().anyMatch(u -> u.getUserName().equals(botName));
                if (!botWasAdded) {
                    telegramEvent.messageEventType(MessageEventType.USER_JOINED_CHAT);
                    telegramEvent.username(newChatMembers.get(0).getUserName());
                } else {
                    // There is an explicit message with "newChatMembers", but we use the service message about change
                    // of member status to find out whether a bot was added to a group
                    return false;
                }
            } else if (update.getMessage().getText() != null) {
                if (update.getMessage().getEntities() != null) {
                    var entity = update.getMessage().getEntities().get(0);
                    if (entity.getText().equals("/start")) {
                        telegramEvent.messageEventType(MessageEventType.USER_STARTED_CHAT);
                        telegramEvent.text("start");

                        // Create Session
                        // This means that using "/start" in a chat, RESETS the current conversation status
                        activeSessions.put(chatId, new Session(chatId, MessageEventType.USER_STARTED_CHAT));
                    } else if (entity.getText().equals("/stop")) {
                        telegramEvent.messageEventType(MessageEventType.USER_LEFT_CHAT);
                        telegramEvent.text("stop");
                        activeSessions.remove(chatId);
                    } else if (entity.getType().equals("bot_command")) {
                        telegramEvent.messageEventType(MessageEventType.BOT_COMMAND);
                        telegramEvent.text(update.getMessage().getEntities().get(0).getText());
                        var args = update.getMessage().getText().split(" ");
                        telegramEvent.commandArguments(Arrays.stream(args).toList().subList(1, args.length));
                    } else {
                        return false;
                    }
                } else if (!update.getMessage().getText().isEmpty()) {
                    telegramEvent.messageEventType(MessageEventType.USER_SENT_MESSAGE);
                    telegramEvent.text(update.getMessage().getText());
                } else {
                    return false;
                }

                var activeSession = activeSessions.get(chatId);
                if (activeSession == null) {
                    activeSession = new Session(chatId, telegramEvent.messageEventType());
                    activeSessions.put(chatId, activeSession);
                    telegramEvent.session(activeSession);
                } else {
                    telegramEvent.session(activeSession);
                }
                telegramEvent.username(update.getMessage().getFrom().getUserName());
            } else {
                return false;
            }

            telegramEvent.user(new TelegramUser(this, chatId));
        } else {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "TelegramComponent{username='%s', activeSessions=%s}".formatted(botName, activeSessions);
    }
}
