package i5.bml.transpiler.bot.events.messenger.telegram;

import i5.bml.transpiler.bot.events.messenger.User;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;

import java.util.Objects;

public record TelegramUser(TelegramComponent telegramComponent, Long chatId) implements User {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TelegramUser) obj;
        return Objects.equals(this.telegramComponent, that.telegramComponent) &&
                Objects.equals(this.chatId, that.chatId);
    }

    @Override
    public String toString() {
        return "TelegramUser[telegramComponent=%s, chatId=%d]".formatted(telegramComponent, chatId);
    }
}
