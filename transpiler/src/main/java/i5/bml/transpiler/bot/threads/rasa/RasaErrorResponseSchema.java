package i5.bml.transpiler.bot.threads.rasa;

import java.util.Objects;

public record RasaErrorResponseSchema(String version, String status, String errorMessage, String reason, Object details,
                                      String help, int code) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RasaErrorResponseSchema) obj;
        return Objects.equals(this.version, that.version) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.errorMessage, that.errorMessage) &&
                Objects.equals(this.reason, that.reason) &&
                Objects.equals(this.details, that.details) &&
                Objects.equals(this.help, that.help) &&
                this.code == that.code;
    }

    @Override
    public String toString() {
        return "RasaErrorResponseSchema[" +
                "version=" + version + ", " +
                "status=" + status + ", " +
                "errorMessage=" + errorMessage + ", " +
                "reason=" + reason + ", " +
                "details=" + details + ", " +
                "help=" + help + ", " +
                "code=" + code + ']';
    }
}
