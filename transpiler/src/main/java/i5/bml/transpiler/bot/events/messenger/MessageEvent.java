package i5.bml.transpiler.bot.events.messenger;

import i5.bml.transpiler.bot.Session;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;

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

    public MessageEvent(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    public MessageEventType getMessageEventType() {
        return messageEventType;
    }

    public void setMessageEventType(MessageEventType messageEventType) {
        this.messageEventType = messageEventType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getCommandArguments() {
        return commandArguments;
    }

    public void setCommandArguments(List<String> commandArguments) {
        this.commandArguments = commandArguments;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public EventSource getEventSource() {
        return eventSource;
    }

    @Override
    public String toString() {
        return "MessageEvent{eventSource=%s, messageEventType=%s, text='%s', commandArguments=%s, session=%s, user=%s, username='%s'}".formatted(eventSource, messageEventType, text, commandArguments, session, user, username);
    }
}
