// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Vector;
import java.util.Arrays;

import org.vngx.jsch.util.Logger;

/**
 * A java.util.logging Logger for JSch.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JSchLogger implements Logger {
    private java.util.logging.Logger logger;

    public JSchLogger(java.util.logging.Logger logger) {
	this.logger = logger;
    }

    // Implement org.vngx.util.Logger

    public boolean isEnabled(Level level) {
	return getLevel(level) == logger.getLevel();
    }

    public void log(Level level, String message) {
	logger.log(getLevel(level), message);
    }

    public void log(Level level, String message, Object... args) {
	logger.log(getLevel(level), message, Arrays.asList(args).toArray());
    }

    public void log(Level level, String message, Throwable exception) {
	logger.log(getLevel(level), message, exception);
    }

    // Private

    java.util.logging.Level getLevel(Level level) {
	switch(level) {
	  case DEBUG:
	    return java.util.logging.Level.FINE;
	  case INFO:
	    return java.util.logging.Level.INFO;
	  case WARN:
	  case ERROR:
	    return java.util.logging.Level.WARNING;
	  case FATAL:
	  default:
	    return java.util.logging.Level.SEVERE;
	}
    }
}
