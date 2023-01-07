package i5.bml.transpiler.bot.config;

import java.util.Objects;

public class BotSettings {

    private String rasaModelName;

    public BotSettings() {}

    public String rasaModelName() {
        return rasaModelName;
    }

    public void rasaModelName(String rasaModelName) {
        this.rasaModelName = rasaModelName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BotSettings) obj;
        return Objects.equals(this.rasaModelName, that.rasaModelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rasaModelName);
    }

    @Override
    public String toString() {
        return "BotSettings[" +
                "rasaModelName=" + rasaModelName + ']';
    }
}
