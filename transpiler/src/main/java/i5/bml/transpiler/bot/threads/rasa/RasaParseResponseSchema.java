package i5.bml.transpiler.bot.threads.rasa;

import java.util.Arrays;
import java.util.Objects;

public final class RasaParseResponseSchema {
    
    private final Entity[] entities;
    
    private final Intent intent;
    
    private final Intent[] intent_ranking;
    
    private final String text;

    public RasaParseResponseSchema(Entity[] entities, Intent intent, Intent[] intent_ranking, String text) {
        this.entities = entities;
        this.intent = intent;
        this.intent_ranking = intent_ranking;
        this.text = text;
    }

    public Entity[] entities() {
        return entities;
    }

    public Intent intent() {
        return intent;
    }

    public Intent[] intentRanking() {
        return intent_ranking;
    }

    public String text() {
        return text;
    }

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
        return Objects.hash(Arrays.hashCode(entities), intent, Arrays.hashCode(intent_ranking), text);
    }

    @Override
    public String toString() {
        return "RasaParseResponseSchema{\n" +
                "entities=" + Arrays.toString(entities) + ",\n" +
                "intent=" + intent + ",\n" +
                "intent_ranking=" + Arrays.toString(intent_ranking) + ",\n" +
                "text=" + text + "\n" +
                '}';
    }

}
