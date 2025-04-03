package org.example.engine.utils;

/**
 * Simple logging utility to avoid using System.out directly
 * and prevent naming conflicts with the ECS System class
 */
public class Logger {
    // Log levels
    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private static LogLevel currentLevel = LogLevel.INFO;
    private static boolean logToConsole = true;

    /**
     * Log a debug message
     */
    public static void debug(String message) {
        if (currentLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            log("DEBUG", message);
        }
    }

    /**
     * Log an info message
     */
    public static void info(String message) {
        if (currentLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            log("INFO", message);
        }
    }

    /**
     * Log a warning message
     */
    public static void warning(String message) {
        if (currentLevel.ordinal() <= LogLevel.WARNING.ordinal()) {
            log("WARNING", message);
        }
    }

    /**
     * Log an error message
     */
    public static void error(String message) {
        if (currentLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            log("ERROR", message);
        }
    }

    /**
     * Log a message with the specified tag
     */
    private static void log(String level, String message) {
        if (logToConsole) {
            // Using java.lang.System to avoid ambiguity
            java.lang.System.out.println("[" + level + "] " + message);
        }

        // Here you could add file logging or other output methods
    }

    /**
     * Set the current log level
     */
    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    /**
     * Enable or disable console logging
     */
    public static void setLogToConsole(boolean enable) {
        logToConsole = enable;
    }
}