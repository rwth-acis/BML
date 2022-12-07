package i5.bml.transpiler.bot.events.messenger.slack;

import i5.bml.transpiler.bot.events.messenger.User;

public record SlackUser(String botToken, String channelId) implements User {

}
