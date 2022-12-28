package i5.bml.transpiler.bot.events.messenger.telegram;

import i5.bml.transpiler.bot.events.messenger.User;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;

import java.util.Objects;

public final class TelegramUser implements User {

    private final TelegramComponent telegramComponent;

    private final Long chatId;

    public TelegramUser(TelegramComponent telegramComponent, Long chatId) {
        this.telegramComponent = telegramComponent;
        this.chatId = chatId;
    }

    public TelegramComponent telegramComponent() {
        return telegramComponent;
    }

    public Long chatId() {
        return chatId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TelegramUser) obj;
        return Objects.equals(this.telegramComponent, that.telegramComponent) &&
                Objects.equals(this.chatId, that.chatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telegramComponent, chatId);
    }

    @Override
    public String toString() {
        return "TelegramUser[telegramComponent=%s, chatId=%d]".formatted(telegramComponent, chatId);
    }
}
