package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.ChannelLeftEvent;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import com.slack.api.model.event.MemberLeftChannelEvent;
import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.Session;
import i5.bml.transpiler.bot.events.Event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

public class SlackBotThread implements Callable<SocketModeClient> {

    private final PriorityBlockingQueue<Event> eventQueue;

    private String botId;
    
    private final String botToken;

    private final Map<String, Session> activeSessions = new HashMap<>();

    public SlackBotThread(PriorityBlockingQueue<Event> eventQueue) {
        this.eventQueue = eventQueue;
        botToken = System.getenv("SLACK_BOT_TOKEN");
    }

    @Override
    public SocketModeClient call() {
        // App expects an env variable: SLACK_BOT_TOKEN
        App app = new App();
        try {
            var authTestResponse = app.getClient().authTest(r -> r.token(botToken));
            botId = authTestResponse.getBotId();
        } catch (IOException | SlackApiException e) {
            // TODO: Proper error handling
            e.printStackTrace();
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
            // SocketModeApp expects an env variable: SLACK_APP_TOKEN
            var socketModeApp = new SocketModeApp(app);
            socketModeApp.startAsync();
            return socketModeApp.getClient();
        } catch (Exception e) {
            // TODO: Proper error handling
            e.printStackTrace();
        }

        // TODO: Proper error handling
        return null;
    }

    public PriorityBlockingQueue<Event> getEventQueue() {
        return eventQueue;
    }

    public String getBotId() {
        return botId;
    }

    public String getBotToken() {
        return botToken;
    }

    public Map<String, Session> getActiveSessions() {
        return activeSessions;
    }
}
