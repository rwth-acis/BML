package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import i5.bml.transpiler.bot.threads.Session;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;

public class MemberJoinedChannelEventHandler extends AbstractSlackHandler implements BoltEventHandler<MemberJoinedChannelEvent> {

    public MemberJoinedChannelEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<MemberJoinedChannelEvent> event, EventContext context) throws SlackApiException, IOException {
        var slackEvent = new MessageEvent(EventSource.SLACK, event.getEventTime());
        if (event.getEvent().getUser().equals(slackBotThread.botId())) {
            slackEvent.setMessageEventType(MessageEventType.BOT_ADDED);
            slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getInviter()));
            slackBotThread.activeSessions().put(event.getEvent().getChannel(),
                    new Session(event.getEvent().getChannel()));
        } else {
            slackEvent.setMessageEventType(MessageEventType.USER_JOINED_CHAT);
            slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getUser()));
        }

        slackEvent.setUser(new SlackUser(slackBotThread.client(), slackBotThread.botToken(), event.getEvent().getChannel()));

        slackBotThread.eventQueue().put(slackEvent);
        return context.ack();
    }
}
