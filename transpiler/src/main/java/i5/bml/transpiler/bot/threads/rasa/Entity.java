package i5.bml.transpiler.bot.threads.rasa;

import java.util.Objects;

public final class Entity {

    private final int start;

    private final int end;

    private final String value;

    private final String entity;

    private final double confidence;

    public Entity(int start, int end, String value, String entity, double confidence) {
        this.start = start;
        this.end = end;
        this.value = value;
        this.entity = entity;
        this.confidence = confidence;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public String value() {
        return value;
    }

    public String entity() {
        return entity;
    }

    public double confidence() {
        return confidence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, value, entity, confidence);
    }

    @Override
    public String toString() {
        return "Entity{\n" +
                "start=" + start + ",\n" +
                "end=" + end + ",\n" +
                "value=" + value + ",\n" +
                "entity=" + entity + ",\n" +
                "confidence=" + confidence + "\n" +
                '}';
    }

}
