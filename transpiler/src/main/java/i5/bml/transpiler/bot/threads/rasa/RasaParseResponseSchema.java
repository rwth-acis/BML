package i5.bml.transpiler.bot.threads.rasa;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public record RasaParseResponseSchema(Entity[] entities, Intent intent, Intent[] intent_ranking, String text) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RasaParseResponseSchema) obj;
        return Arrays.equals(this.entities, that.entities) &&
                Objects.equals(this.intent, that.intent) &&
                Arrays.equals(this.intent_ranking, that.intent_ranking) &&
                Objects.equals(this.text, that.text);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(intent, text);
        result = 31 * result + Arrays.hashCode(entities);
        result = 31 * result + Arrays.hashCode(intent_ranking);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Intent i : intent_ranking) {
            sb.append("    ").append(i).append(",\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        return "RasaParseResponseSchema[\n" +
                "  entities=" + Arrays.toString(entities) + ",\n" +
                "  intent=" + intent + ",\n" +
                "  intent_ranking=[\n" + sb.toString() + "\n  ]\n" +
                "  text=" + text + "\n]";
    }
}
