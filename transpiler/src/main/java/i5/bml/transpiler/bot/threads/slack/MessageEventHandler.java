package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import i5.bml.transpiler.bot.threads.Session;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;

public class MessageEventHandler extends AbstractSlackHandler implements BoltEventHandler<com.slack.api.model.event.MessageEvent> {

    public MessageEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<com.slack.api.model.event.MessageEvent> event, EventContext context) throws IOException, SlackApiException {
        if (event.getEvent().getText().startsWith("/")) {
            return context.ack();
        }

        var slackEvent = new MessageEvent(EventSource.SLACK, event.getEventTime());
        var session = slackBotThread.activeSessions().get(event.getEvent().getChannel());
        if (session == null) {
            session = new Session(event.getEvent().getChannel());
            slackBotThread.activeSessions().put(event.getEvent().getChannel(), session);
            slackEvent.messageEventType(MessageEventType.USER_STARTED_CHAT);
        } else {
            slackEvent.messageEventType(MessageEventType.USER_SENT_MESSAGE);
        }

        slackEvent.session(session);
        slackEvent.username(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getUser()));
        slackEvent.text(event.getEvent().getText());

        slackEvent.user(new SlackUser(slackBotThread.client(), slackBotThread.botToken(), event.getEvent().getChannel()));

        slackBotThread.eventQueue().put(slackEvent);
        return context.ack();
    }
}
