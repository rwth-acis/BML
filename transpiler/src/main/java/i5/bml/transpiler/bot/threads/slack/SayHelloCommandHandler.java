package i5.bml.transpiler.bot.threads.slack;

import com.google.gson.JsonObject;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import i5.bml.transpiler.bot.events.EventSource;
import i5.bml.transpiler.bot.events.messenger.MessageEvent;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.bot.events.messenger.slack.SlackUser;

import java.io.IOException;
import java.util.Arrays;

public class SayHelloCommandHandler extends AbstractSlackHandler implements SlashCommandHandler {

    public SayHelloCommandHandler(SlackBotThread slackBotThread) {
        super(slackBotThread);
    }

    @Override
    public Response apply(SlashCommandRequest slashCommandRequest, SlashCommandContext context) throws IOException, SlackApiException {
        // We use the arrival time in seconds such that is matches the unix time unit used by Slack, Telegram, etc.
        var slackEvent = new MessageEvent(EventSource.SLACK, System.currentTimeMillis() / 1_000);
        slackEvent.setMessageEventType(MessageEventType.BOT_COMMAND);
        slackEvent.setUsername(fetchDisplayName(context.client(), slackBotThread.getBotToken(), context.getRequestUserId()));
        slackEvent.setUser(new SlackUser(slackBotThread.getBotToken(), context.getChannelId()));
        slackEvent.setCommandArguments(Arrays.stream(slashCommandRequest.getPayload().getText().split(" ")).toList());
        slackEvent.setText(slashCommandRequest.getPayload().getCommand());
        slackEvent.setSession(slackBotThread.getActiveSessions().get(context.getChannelId()));
        slackEvent.setUser(new SlackUser(slackBotThread.getBotToken(), context.getChannelId()));
        slackBotThread.getEventQueue().put(slackEvent);

        JsonObject responseType = new JsonObject();
        responseType.addProperty("response_type", "in_channel");
        return context.ack(responseType);
    }
}
