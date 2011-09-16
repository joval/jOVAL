// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Vector;
import java.util.Arrays;

import org.vngx.jsch.util.Logger;

import org.slf4j.cal10n.LocLogger;

/**
 * An SLF4J bridge for JSch.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JSchLogger implements Logger {
    private LocLogger logger;

    public JSchLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement org.vngx.util.Logger

    public boolean isEnabled(Level level) {
	return true;
    }

    public void log(Level level, String message) {
	switch(level) {
	  case DEBUG:
	    logger.debug(message);
	    break;
	  case WARN:
	    logger.warn(message);
	    break;
	  case ERROR:
	  case FATAL:
	    logger.error(message);
	    break;
	  case INFO:
	  default:
	    logger.info(message);
	}
    }

    public void log(Level level, String message, Object... args) {
	Object[] oa = Arrays.asList(args).toArray();
	switch(level) {
	  case DEBUG:
	    logger.debug(message, oa);
	    break;
	  case WARN:
	    logger.warn(message, oa);
	    break;
	  case ERROR:
	  case FATAL:
	    logger.error(message, oa);
	    break;
	  case INFO:
	  default:
	    logger.info(message, oa);
	}
    }

    public void log(Level level, String message, Throwable exception) {
	switch(level) {
	  case DEBUG:
	    logger.debug(message, exception);
	    break;
	  case WARN:
	    logger.warn(message, exception);
	    break;
	  case ERROR:
	  case FATAL:
	    logger.error(message, exception);
	    break;
	  case INFO:
	  default:
	    logger.info(message, exception);
	}
    }
}
