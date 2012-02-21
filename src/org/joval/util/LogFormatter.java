// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Formatter;

/**
 * A utility class for formatting java.util.logging log messages.
 * NB: It will override any pre-existing JUL configuration.
 *
 * @author David A. Solin
 */
public class LogFormatter extends Formatter {
    public enum Type {FILE, CONSOLE;}

    private static final String LF = System.getProperty("line.separator");
    static {
	try {
	    InputStream in = new ByteArrayInputStream("java.util.logging.handlers=".getBytes());
	    LogManager.getLogManager().readConfiguration(in);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public static String toString(Throwable t) {
	StringBuffer sb = new StringBuffer(t.getClass().getName());
	sb.append(":").append(t.getMessage()).append(LF);
	StackTraceElement[] ste = t.getStackTrace();
	for (int i=0; i < ste.length; i++) {
	    sb.append("    ").append(ste[i].toString()).append(LF);
	}
	Throwable cause = t.getCause();
	if (cause != null) {
	    sb.append("caused by:").append(LF);
	    sb.append(toString(cause));
	}
	return sb.toString();
    }

    public static Level toLevel(String logLevel) {
	Level level = Level.INFO;
	if (logLevel != null) {
	    if (logLevel.equalsIgnoreCase("info")) {
		level = Level.INFO;
	    } else if (logLevel.equalsIgnoreCase("finest")) {
		level = Level.FINEST;
	    } else if (logLevel.equalsIgnoreCase("finer")) {
		level = Level.FINER;
	    } else if (logLevel.equalsIgnoreCase("warning")) {
		level = Level.WARNING;
	    } else if (logLevel.equalsIgnoreCase("severe")) {
		level = Level.SEVERE;
	    } else if (logLevel.equalsIgnoreCase("off")) {
		level = Level.OFF;
	    }
	}
	return level;
    }

    /**
     * Set up a pair of Handlers for jOVAL system logging events, one logging to the specified logFile and the other to the
     * console, at the indicated logging level, and return a Logger configured to use them.
     */
    public static Logger createDuplex(File logFile, Level level) throws IOException {
	Handler logfileHandler = new FileHandler(logFile.getPath(), false);
	logfileHandler.setFormatter(new LogFormatter(Type.FILE));
	logfileHandler.setLevel(level);

	Handler consoleHandler = new ConsoleHandler();
	consoleHandler.setFormatter(new LogFormatter(Type.CONSOLE));
	consoleHandler.setLevel(level);

	Logger logger = Logger.getLogger(JOVALSystem.getLogger().getName());
	logger.setLevel(level);
	logger.addHandler(logfileHandler);
	logger.addHandler(consoleHandler);
	return logger;
    }

    private Type type;

    /**
     * Create a new LogFormatter of the specified type.
     */
    public LogFormatter(Type type) {
	super();
	this.type = type;
    }

    // Implement abstract methods from Formatter

    public String format(LogRecord record) {
	StringBuffer line = new StringBuffer();

	switch(type) {
	  case FILE:
	    line.append(currentDateString());
	    line.append(" - ");
	    line.append(record.getLevel().getName());
	    line.append(" - ");
	    line.append(record.getMessage());
	    line.append(LF);
	    break;

	  case CONSOLE:
	  default:
	    line.append(record.getMessage());
	    line.append(LF);
	    break;
	}

	Throwable thrown = record.getThrown();
	if (thrown != null) {
	    line.append(toString(thrown));
	}

	return line.toString();
    }

    // Private

    private static String currentDateString() {
	StringBuffer sb = new StringBuffer();
	Calendar date = new GregorianCalendar();
	sb.append(date.get(Calendar.YEAR)).append(".");
	int month = 1 + date.get(Calendar.MONTH);
	sb.append(pad(month)).append(".");
	sb.append(pad(date.get(Calendar.DAY_OF_MONTH))).append(" ");
	sb.append(pad(date.get(Calendar.HOUR_OF_DAY))).append(":");
	sb.append(pad(date.get(Calendar.MINUTE))).append(":");
	sb.append(pad(date.get(Calendar.SECOND))).append(".");
	sb.append(pad(date.get(Calendar.MILLISECOND), 3));
	return sb.toString();
    }

    private static String pad(int val) {
	return pad(val, 2);
    }

    private static String pad(int val, int width) {
	StringBuffer sb = new StringBuffer();
	for (int i=(width-1); i > 0; i--) {
	    if (val < Math.pow(10, i)) {
		sb.append("0");
	    } else {
		break;
	    }
	}
	sb.append(Integer.toString(val));
	return sb.toString();
    }
}
