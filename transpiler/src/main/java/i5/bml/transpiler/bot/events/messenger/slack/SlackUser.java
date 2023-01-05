package i5.bml.transpiler.bot.events.messenger.slack;

import com.slack.api.socket_mode.SocketModeClient;
import i5.bml.transpiler.bot.events.messenger.User;

import java.util.Objects;

public record SlackUser(SocketModeClient slackClient, String botToken, String channelId) implements User {

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
    public String toString() {
        return "SlackUser[slackClient=%s, botToken=%s, channelId=%s]".formatted(slackClient, botToken, channelId);
    }
}
