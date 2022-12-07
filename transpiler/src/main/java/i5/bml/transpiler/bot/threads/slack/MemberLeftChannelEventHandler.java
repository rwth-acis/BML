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
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;

public class MemberLeftChannelEventHandler extends AbstractSlackHandler implements BoltEventHandler<MemberLeftChannelEvent> {

    public MemberLeftChannelEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<MemberLeftChannelEvent> event, EventContext context) throws IOException, SlackApiException {
        var slackEvent = new MessageEvent(EventSource.SLACK);
        slackEvent.setMessageEventType(MessageEventType.USER_LEFT_CHANNEL);
        slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.getBotToken(), event.getEvent().getUser()));

        slackEvent.setUser(new SlackUser(slackBotThread.getBotToken(), event.getEvent().getChannel()));

        slackBotThread.getEventQueue().offer(slackEvent);
        return context.ack();
    }
}
