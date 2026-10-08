package net.riftwatch.rift_quests.load;

public record Problem(Severity severity, String file, String field, String message) {
    public enum Severity {
        ERROR,
        WARNING
    }

    @Override
    public String toString() {
        return field.isEmpty() ? file + ": " + message : file + " " + field + ": " + message;
    }
}
