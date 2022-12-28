package i5.bml.transpiler.bot.events.messenger.slack;

import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.events.messenger.User;

import java.util.Objects;

public final class SlackUser implements User {

    private final SocketModeClient slackClient;

    private final String botToken;

    private final String channelId;

    public SlackUser(SocketModeClient slackClient, String botToken, String channelId) {
        this.slackClient = slackClient;
        this.botToken = botToken;
        this.channelId = channelId;
    }

    public SocketModeClient slackClient() {
        return slackClient;
    }

    public String botToken() {
        return botToken;
    }

    public String channelId() {
        return channelId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SlackUser) obj;
        return Objects.equals(this.slackClient, that.slackClient) &&
                Objects.equals(this.botToken, that.botToken) &&
                Objects.equals(this.channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slackClient, botToken, channelId);
    }

    @Override
    public String toString() {
        return "SlackUser[slackClient=%s, botToken=%s, channelId=%s]".formatted(slackClient, botToken, channelId);
    }
}
