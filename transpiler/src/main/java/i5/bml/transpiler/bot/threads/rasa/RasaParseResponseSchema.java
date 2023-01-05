package i5.bml.transpiler.bot.threads.rasa;

import java.util.Arrays;
import java.util.Objects;

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
    public String toString() {
        return "RasaParseResponseSchema[" +
                "entities=" + Arrays.toString(entities) + ", " +
                "intent=" + intent + ", " +
                "intent_ranking=" + Arrays.toString(intent_ranking) + ", " +
                "text=" + text + ']';
    }
}
