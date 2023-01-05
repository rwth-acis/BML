package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;
import i5.bml.transpiler.bot.threads.Session;

import java.io.IOException;

public class MemberJoinedChannelEventHandler extends AbstractSlackHandler implements BoltEventHandler<MemberJoinedChannelEvent> {

    public MemberJoinedChannelEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<MemberJoinedChannelEvent> event, EventContext context) throws SlackApiException, IOException {
        var slackEvent = new MessageEvent(EventSource.SLACK, event.getEventTime());
        if (event.getEvent().getUser().equals(slackBotThread.botId())) {
            slackEvent.messageEventType(MessageEventType.BOT_ADDED);
            slackEvent.username(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getInviter()));
            slackBotThread.activeSessions().put(event.getEvent().getChannel(),
                    new Session(event.getEvent().getChannel(), MessageEventType.BOT_ADDED));
        } else {
            slackEvent.messageEventType(MessageEventType.USER_JOINED_CHAT);
            slackEvent.username(fetchDisplayName(context.client(), slackBotThread.botToken(), event.getEvent().getUser()));
        }

        slackEvent.user(new SlackUser(slackBotThread.client(), slackBotThread.botToken(), event.getEvent().getChannel()));

        slackBotThread.eventQueue().put(slackEvent);
        return context.ack();
    }
}
