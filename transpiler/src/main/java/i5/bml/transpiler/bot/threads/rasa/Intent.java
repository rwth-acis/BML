package i5.bml.transpiler.bot.threads.rasa;

import java.util.Objects;

public final class Intent {
    
    private final double confidence;
    
    private final String name;

    public Intent(double confidence, String name) {
        this.confidence = confidence;
        this.name = name;
    }

    public double confidence() {
        return confidence;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Intent) obj;
        return Double.doubleToLongBits(this.confidence) == Double.doubleToLongBits(that.confidence) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confidence, name);
    }

    @Override
    public String toString() {
        return "Intent{\n" +
                "confidence=" + confidence + ",\n" +
                "name=" + name + "\n" +
                '}';
    }

}
