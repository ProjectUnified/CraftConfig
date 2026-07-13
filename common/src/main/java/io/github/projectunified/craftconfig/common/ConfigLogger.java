package io.github.projectunified.craftconfig.common;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin logger provider for CraftConfig.
 * By default, logs to {@link Logger}.
 * Set a custom handler via {@link #set(Handler)} to redirect logs.
 */
public final class ConfigLogger {

    private static volatile Handler handler;

    private ConfigLogger() {
    }

    /**
     * Set a custom log handler.
     *
     * @param logHandler the handler, or null to reset to default
     */
    public static void set(Handler logHandler) {
        handler = logHandler;
    }

    /**
     * Log a message at WARNING level
     *
     * @param clazz   the calling class
     * @param message the message
     */
    public static void warn(Class<?> clazz, String message) {
        log(LogLevel.WARN, clazz, message, null);
    }

    /**
     * Log a message with throwable at WARNING level
     *
     * @param clazz     the calling class
     * @param message   the message
     * @param throwable the throwable
     */
    public static void warn(Class<?> clazz, String message, Throwable throwable) {
        log(LogLevel.WARN, clazz, message, throwable);
    }

    /**
     * Log a message with throwable
     *
     * @param level     the log level
     * @param clazz     the calling class
     * @param message   the message
     * @param throwable the throwable
     */
    public static void log(LogLevel level, Class<?> clazz, String message, Throwable throwable) {
        Handler h = handler;
        if (h != null) {
            h.handle(level, clazz, message, throwable);
        } else {
            Logger logger = Logger.getLogger(clazz.getName());
            Level julLevel;
            switch (level) {
                case DEBUG:
                    julLevel = Level.FINE;
                    break;
                case WARN:
                    julLevel = Level.WARNING;
                    break;
                case ERROR:
                    julLevel = Level.SEVERE;
                    break;
                default:
                    julLevel = Level.INFO;
                    break;
            }
            if (throwable != null) {
                logger.log(julLevel, message, throwable);
            } else {
                logger.log(julLevel, message);
            }
        }
    }

    /**
     * Log levels for CraftConfig
     */
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * The handler interface for processing log messages
     */
    @FunctionalInterface
    public interface Handler {
        /**
         * Handle a log message
         *
         * @param level     the log level
         * @param clazz     the calling class
         * @param message   the message
         * @param throwable the throwable, may be null
         */
        void handle(LogLevel level, Class<?> clazz, String message, Throwable throwable);
    }
}
