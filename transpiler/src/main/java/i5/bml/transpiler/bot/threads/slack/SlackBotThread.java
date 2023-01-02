package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.ChannelLeftEvent;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import com.slack.api.model.event.MemberLeftChannelEvent;
import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.threads.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class SlackBotThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackBotThread.class);

    private final PriorityBlockingQueue<Event> eventQueue;

    private String botId;
    
    private final String botToken;

    private final String appToken;

    private SocketModeClient client;

    private final Map<String, Session> activeSessions = new HashMap<>();

    public SlackBotThread(PriorityBlockingQueue<Event> eventQueue, String botToken, String appToken) {
        this.eventQueue = eventQueue;
        this.botToken = botToken;
        this.appToken = appToken;
    }

    @Override
    public void run() {
        var app = new App();
        try {
            var authTestResponse = app.getClient().authTest(r -> r.token(botToken));
            botId = authTestResponse.getBotId();
        } catch (IOException | SlackApiException e) {
            LOGGER.error("Failed to retrieve `botId` via `authTest`", e);
            // We cannot run the bot without the botId
            return;
        }

        /*
         * Register handler for slack events that are NOT commands. The job of a handler is the enqueueing of a new MessageEvent with
         * the respective information from the slack event.
         */
        // Event: USER_SENT_MESSAGE
        app.event(com.slack.api.model.event.MessageEvent.class, new MessageEventHandler(this));
        // Event: USER_JOINED -> We do not need MessageChannelJoinEvent since this event is more precise
        app.event(MemberJoinedChannelEvent.class, new MemberJoinedChannelEventHandler(this));
        // Event: USER_LEFT
        app.event(MemberLeftChannelEvent.class, new MemberLeftChannelEventHandler(this));
        // Event: BOT_REMOVED
        app.event(ChannelLeftEvent.class, new ChannelLeftEventHandler(this));

        // Register command handler
        app.command("/sayhello", new SayHelloCommandHandler(this));

        try {
            var socketModeApp = new SocketModeApp(appToken, app);
            socketModeApp.startAsync();
            client = socketModeApp.getClient();
            LOGGER.info("Successfully initialized Slack bot");
        } catch (Exception e) {
            LOGGER.error("Connecting with Slack failed", e);
        }
    }

    public String botId() {
        return botId;
    }

    public PriorityBlockingQueue<Event> eventQueue() {
        return eventQueue;
    }

    public String botToken() {
        return botToken;
    }

    public SocketModeClient client() {
        return client;
    }

    public Map<String, Session> activeSessions() {
        return activeSessions;
    }
}
