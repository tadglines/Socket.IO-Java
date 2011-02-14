package com.glines.socketio.util;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * @author Mathieu Carbou
 */

/**
 * @author Mathieu Carbou
 */
public final class JdkOverLog4j extends Handler {

    private static final Map<java.util.logging.Level, Level> LEVELS_JDK_TO_LOG4J = new HashMap<java.util.logging.Level, Level>() {
        private static final long serialVersionUID = 4627902405916164098L;

        {
            put(java.util.logging.Level.OFF, Level.OFF);
            put(java.util.logging.Level.SEVERE, Level.ERROR);
            put(java.util.logging.Level.WARNING, Level.WARN);
            put(java.util.logging.Level.INFO, Level.INFO);
            put(java.util.logging.Level.CONFIG, Level.INFO);
            put(java.util.logging.Level.FINE, Level.DEBUG);
            put(java.util.logging.Level.FINER, Level.DEBUG);
            put(java.util.logging.Level.FINEST, Level.DEBUG);
            put(java.util.logging.Level.ALL, Level.ALL);
        }
    };

    private static final Map<Level, java.util.logging.Level> LEVELS_LOG4J_TO_JDK = new HashMap<Level, java.util.logging.Level>() {
        private static final long serialVersionUID = 1880156903738858787L;

        {
            put(Level.OFF, java.util.logging.Level.OFF);
            put(Level.FATAL, java.util.logging.Level.SEVERE);
            put(Level.ERROR, java.util.logging.Level.SEVERE);
            put(Level.WARN, java.util.logging.Level.WARNING);
            put(Level.INFO, java.util.logging.Level.INFO);
            put(Level.DEBUG, java.util.logging.Level.FINE);
            put(Level.TRACE, java.util.logging.Level.FINE);
            put(Level.ALL, java.util.logging.Level.ALL);
        }
    };

    @Override
    public void publish(LogRecord record) {
        // normalize levels
        Logger log4jLogger = Logger.getLogger(record.getLoggerName());
        Level log4jLevel = log4jLogger.getEffectiveLevel();
        java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(record.getLoggerName());
        java.util.logging.Level expectedJdkLevel = LEVELS_LOG4J_TO_JDK.get(log4jLevel);
        if (expectedJdkLevel == null)
            throw new AssertionError("Level not supported yet - have a bug !" + log4jLevel);
        if (!expectedJdkLevel.equals(jdkLogger.getLevel())) {
            jdkLogger.setLevel(expectedJdkLevel);
        }
        log4jLogger.log(record.getLoggerName(), LEVELS_JDK_TO_LOG4J.get(record.getLevel()), record.getMessage(), record.getThrown());
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    public static void install() {
        LogManager.getLogManager().reset();
        LogManager.getLogManager().getLogger("").addHandler(new JdkOverLog4j());
        LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.ALL);
    }
}
