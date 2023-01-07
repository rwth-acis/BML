package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.threads.Session;

import java.util.ArrayList;
import java.util.List;

public class MessageEvent implements Event {

    private final EventSource eventSource;

    /**
     * See {@link MessageEventType} for a detailed explanation of the messenger event types.
     */
    protected MessageEventType messageEventType;

    /**
     * Contents of the received message.
     */
    protected String text = "";

    private String intent;

    private String entity;

    /**
     *
     */
    private List<String> commandArguments = new ArrayList<>();

    /**
     *
     */
    protected Session session;

    /**
     *
     */
    protected User user;

    /**
     * Author of the received message.
     * <p>
     * In case of {@link MessageEventType#BOT_ADDED} or {@link MessageEventType#BOT_REMOVED} it is the user that
     * added or removed the bot.
     */
    protected String username;

    private final long arrivalTime;

    public MessageEvent(EventSource eventSource, long arrivalTime) {
        this.eventSource = eventSource;
        this.arrivalTime = arrivalTime;
    }

    public MessageEventType messageEventType() {
        return messageEventType;
    }

    public void messageEventType(MessageEventType messageEventType) {
        this.messageEventType = messageEventType;
    }

    public String text() {
        return text;
    }

    public void text(String text) {
        this.text = text;
    }

    public String intent() {
        return intent;
    }

    public void intent(String intent) {
        this.intent = intent;
    }

    public String entity() {
        return entity;
    }

    public void entity(String entity) {
        this.entity = entity;
    }

    public List<String> commandArguments() {
        return commandArguments;
    }

    public void commandArguments(List<String> commandArguments) {
        this.commandArguments = commandArguments;
    }

    public Session session() {
        return session;
    }

    public void session(Session session) {
        this.session = session;
    }

    public User user() {
        return user;
    }

    public void user(User user) {
        this.user = user;
    }

    public String username() {
        return username;
    }

    public void username(String username) {
        this.username = username;
    }

    @Override
    public EventSource eventSource() {
        return eventSource;
    }

    @Override
    public long arrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return "MessageEvent{\n" +
                "  eventSource=" + eventSource + " \n" +
                "  messageEventType=" + messageEventType + " \n" +
                "  text='" + text + '\'' + " \n" +
                "  intent='" + intent + '\'' + " \n" +
                "  entity='" + entity + '\'' + " \n" +
                "  commandArguments=" + commandArguments + " \n" +
                "  session=" + session + " \n" +
                "  user=" + user + " \n" +
                "  username='" + username + '\'' + " \n" +
                "  arrivalTime=" + arrivalTime + " \n" +
                '}';
    }
}
