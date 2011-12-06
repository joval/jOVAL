// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;

/**
 * A test class that runs jOVAL through all the relevant OVAL test content and generates a report.
 *
 * @author David A. Solin
 */
public class LogFormatter extends Formatter {
    private static final String LF = System.getProperty("line.separator");
    private static File resources = new File("rsrc");
    static {
	try {
	    LogManager.getLogManager().readConfiguration(new FileInputStream(new File(resources, "logging.properties")));
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

    LogFormatter() {
	super();
    }

    // Implement abstract methods from Formatter

    public String format(LogRecord record) {
	StringBuffer line = new StringBuffer(currentDateString());
	line.append(" - ");
	line.append(record.getLevel().getName());
	line.append(" - ");
	line.append(record.getMessage());
	line.append(LF);

	Throwable thrown = record.getThrown();
	if (thrown != null) {
	    line.append(toString(thrown));
	}
	return line.toString();
    }

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
