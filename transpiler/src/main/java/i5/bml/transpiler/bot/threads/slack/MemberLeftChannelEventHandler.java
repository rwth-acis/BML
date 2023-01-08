package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MemberLeftChannelEvent;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;

import java.io.IOException;

public class MemberLeftChannelEventHandler extends AbstractSlackHandler implements BoltEventHandler<MemberLeftChannelEvent> {

    public MemberLeftChannelEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<MemberLeftChannelEvent> event, EventContext context) throws IOException, SlackApiException {
        var slackEvent = new MessageEvent(EventSource.SLACK, event.getEventTime());
        slackEvent.messageEventType(MessageEventType.USER_LEFT_CHAT);
        slackEvent.username(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getUser()));

        slackEvent.user(new SlackUser(slackBotThread.client(), slackBotThread.botToken(), event.getEvent().getChannel()));

        slackBotThread.eventQueue().put(slackEvent);
        return context.ack();
    }
}
