package i5.bml.transpiler.bot.threads.rasa;

import java.util.Objects;

public record Intent(double confidence, String name) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Intent) obj;
        return Double.doubleToLongBits(this.confidence) == Double.doubleToLongBits(that.confidence) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public String toString() {
        return "Intent[" +
                "confidence=" + confidence + ", " +
                "name=" + name + ']';
    }
}
