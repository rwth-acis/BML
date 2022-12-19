package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import i5.bml.transpiler.bot.Session;
import i5.bml.transpiler.bot.events.Event;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

public class MemberJoinedChannelEventHandler extends AbstractSlackHandler implements BoltEventHandler<MemberJoinedChannelEvent> {

    public MemberJoinedChannelEventHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(EventsApiPayload<MemberJoinedChannelEvent> event, EventContext context) throws SlackApiException, IOException {
        var slackEvent = new MessageEvent(EventSource.SLACK, event.getEventTime());
        if (event.getEvent().getUser().equals(slackBotThread.getBotId())) {
            slackEvent.setMessageEventType(MessageEventType.BOT_ADDED);
            slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.getBotToken(), event.getEvent().getInviter()));
            slackBotThread.getActiveSessions().put(event.getEvent().getChannel(),
                    new Session(event.getEvent().getChannel()));
        } else {
            slackEvent.setMessageEventType(MessageEventType.USER_JOINED_CHANNEL);
            slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.getBotToken(), event.getEvent().getUser()));
        }

        slackEvent.setUser(new SlackUser(slackBotThread.getBotToken(), event.getEvent().getChannel()));

        slackBotThread.getEventQueue().put(slackEvent);
        return context.ack();
    }
}
