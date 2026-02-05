package dev.onlydarkness.utils.modules.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEvent {
    public enum Level { INFO, WARN, ERROR, DEBUG, FATAL }

    private final Level level;
    private final String source;
    private final String message;
    private final String time;

    public LogEvent(Level level, String source, String message) {
        this.level = level;
        this.source = source;
        this.message = message;
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public Level getLevel() {return level;}

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s]: %s", time, level, source, message);
    }
}
