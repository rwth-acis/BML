package i5.bml.transpiler.bot.threads.rasa;

import java.util.Objects;

public record Entity(int start, int end, String value, String entity, double confidence) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Entity) obj;
        return this.start == that.start &&
                this.end == that.end &&
                Objects.equals(this.value, that.value) &&
                Objects.equals(this.entity, that.entity) &&
                Double.doubleToLongBits(this.confidence) == Double.doubleToLongBits(that.confidence);
    }

    @Override
    public String toString() {
        return "Entity[" +
                "start=" + start + ", " +
                "end=" + end + ", " +
                "value=" + value + ", " +
                "entity=" + entity + ", " +
                "confidence=" + confidence + ']';
    }
}
