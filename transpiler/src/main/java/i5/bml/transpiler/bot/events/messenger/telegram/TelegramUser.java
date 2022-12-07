package i5.bml.transpiler.bot.events.messenger.telegram;

import i5.bml.transpiler.bot.events.messenger.User;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;

public record TelegramUser(TelegramComponent telegramComponent, Long chatId) implements User {

}
