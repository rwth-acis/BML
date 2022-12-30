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
        slackEvent.messageEventType(MessageEventType.BOT_COMMAND);
        slackEvent.username(fetchDisplayName(context.client(), slackBotThread.botToken(), context.getRequestUserId()));
        slackEvent.user(new SlackUser(slackBotThread.client(), slackBotThread.botToken(), context.getChannelId()));
        slackEvent.commandArguments(Arrays.stream(slashCommandRequest.getPayload().getText().split(" ")).toList());
        slackEvent.text(slashCommandRequest.getPayload().getCommand());
        slackEvent.session(slackBotThread.activeSessions().get(context.getChannelId()));
        slackBotThread.eventQueue().put(slackEvent);

        JsonObject responseType = new JsonObject();
        responseType.addProperty("response_type", "in_channel");
        return context.ack(responseType);
    }
}
