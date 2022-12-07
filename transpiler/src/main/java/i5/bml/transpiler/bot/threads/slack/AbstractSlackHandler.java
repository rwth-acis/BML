package i5.bml.transpiler.bot.threads.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;

import java.io.IOException;

public abstract class AbstractSlackHandler {

    protected final SlackBotThread slackBotThread;

    protected AbstractSlackHandler(SlackBotThread slackBotThread) {
        this.slackBotThread = slackBotThread;
    }

    protected String fetchDisplayName(MethodsClient client, String botToken, String userId) throws SlackApiException, IOException {
        return client.usersInfo(r -> r.token(botToken).user(userId)).getUser().getName();
    }
}
