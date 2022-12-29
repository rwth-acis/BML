package i5.bml.transpiler.bot.dialogue;

import java.util.Objects;

public final class RasaErrorResponseSchema {

    private final String version;

    private final String status;

    private final String errorMessage;

    private final String reason;

    private final Object details;

    private final String help;

    private final int code;

    public RasaErrorResponseSchema(String version, String status, String errorMessage, String reason, Object details, String help, int code) {
        this.version = version;
        this.status = status;
        this.errorMessage = errorMessage;
        this.reason = reason;
        this.details = details;
        this.help = help;
        this.code = code;
    }

    public String version() {
        return version;
    }

    public String status() {
        return status;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String reason() {
        return reason;
    }

    public Object details() {
        return details;
    }

    public String help() {
        return help;
    }

    public int code() {
        return code;
    }

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
    public int hashCode() {
        return Objects.hash(version, status, errorMessage, reason, details, help, code);
    }

    @Override
    public String toString() {
        return "RasaErrorResponseSchema{" +
                "version='" + version + "'\n" +
                ", status='" + status + "'\n" +
                ", errorMessage='" + errorMessage + "'\n" +
                ", reason='" + reason + "'\n" +
                ", details=" + details +
                ", help='" + help + "'\n" +
                ", code=" + code + "'\n" +
                '}';
    }
}
