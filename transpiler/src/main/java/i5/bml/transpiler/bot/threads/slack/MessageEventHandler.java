package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import i5.bml.transpiler.bot.Session;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

public class MessageEventHandler extends AbstractSlackHandler implements BoltEventHandler<com.slack.api.model.event.MessageEvent> {

    public MessageEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<com.slack.api.model.event.MessageEvent> event, EventContext context) throws IOException, SlackApiException {
        if (event.getEvent().getText().startsWith("/")) {
            return context.ack();
        }

        var slackEvent = new MessageEvent(EventSource.SLACK);
        var session = slackBotThread.getActiveSessions().get(event.getEvent().getChannel());
        if (session == null) {
            session = new Session(event.getEvent().getChannel());
            slackBotThread.getActiveSessions().put(event.getEvent().getChannel(), session);
            slackEvent.setMessageEventType(MessageEventType.USER_STARTED_CHAT);
        } else {
            slackEvent.setMessageEventType(MessageEventType.USER_SENT_MESSAGE);
        }

        slackEvent.setSession(session);
        slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.getBotToken(), event.getEvent().getUser()));
        slackEvent.setText(event.getEvent().getText());

        slackEvent.setUser(new SlackUser(slackBotThread.getBotToken(), event.getEvent().getChannel()));

        slackBotThread.getEventQueue().offer(slackEvent);
        return context.ack();
    }
}
