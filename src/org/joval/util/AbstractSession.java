// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimerTask;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamLogger;
import org.joval.util.JOVALSystem;

/**
 * Base class for the local and remote Windows and Unix ISession implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractSession extends AbstractBaseSession implements ISession {
    protected File cwd;
    protected IEnvironment env;
    protected IFilesystem fs;

    /**
     * Create an ISession with no workspace to store state information, i.e., for a local ISession.
     */
    protected AbstractSession() {
	super();
    }

    // Implement ILoggable

    /**
     * Here, we harmonize the IFilesystem's logger with the ISession's logger.
     */
    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (fs != null) {
	    fs.setLogger(logger);
	}
    }

    // Implement ISession

    public void setWorkingDir(String path) {
	cwd = new File(path);
    }

    public IEnvironment getEnvironment() {
	return env;
    }

    public IFilesystem getFilesystem() {
	return fs;
    }

    /**
     * Here, we provide an implementation for local ISessions.
     */
    @Override
    public IProcess createProcess(String command) throws Exception {
	return new JavaProcess(command);
    }

    // All the abstract methods, for reference

    public abstract boolean connect();

    public abstract void disconnect();

    public abstract String getHostname();

    public abstract Type getType();

    public abstract SystemInfoType getSystemInfo();

    // Private

    private static final char NULL = (char)0;
    private static final char SQ = '\'';
    private static final char DQ = '\"';
    private static final char ESC = '\\';

    private int pid = 1;

    class JavaProcess implements IProcess {
	String command;
	Process p;
	int pid;
	StreamLogger debugIn, debugErr;

	JavaProcess(String command) {
	    this.command = command;
	    this.pid = AbstractSession.this.pid++;
	}

	// Implement IProcess

	public String getCommand() {
	    return command;
	}

	public void setInteractive(boolean interactive) {
	}

	/**
	 * Complex commands may contain combinations of quotes and escapes.  Since commands run locally
	 * are not interpreted by a shell, and since Java's parsing of a command-line String is overly
	 * simplistic, we process the command in this method to break it down properly into its constituent
	 * tokens.
	 */
	public void start() throws Exception {
	    ArrayList<String> args = new ArrayList<String>();
	    int ptr=0;
	    int len = command.length();
	    char context = NULL, last = NULL;
	    boolean escaped = false;
	    StringBuffer arg = new StringBuffer();
	    while (ptr < len) {
		char ch = command.charAt(ptr++);
		switch(ch) {
		  case SQ:
		  case DQ:
		    if (escaped) {
			arg.append(ch);
		    } else if (context == NULL) {
			context = ch;
		    } else if (context == ch) {
			args.add(arg.toString());
			arg = new StringBuffer();
			context = NULL;
		    } else {
			arg.append(ch);
		    }
		    break;

		  case ' ':
		  case '\t':
		  case '\n':
		    if (context != NULL) {
			arg.append(ch);
		    } else if (arg.length() > 0) {
			args.add(arg.toString());
			arg = new StringBuffer();
		    }
		    break;

		  default:
		    arg.append(ch);
		    break;
		}

		// determine whether the next character is escaped
		if (ch == ESC) {
		    if (last == ESC) {
			escaped = !escaped;
		    } else {
			escaped = true;
		    }
		} else {
		    escaped = false;
		}
		last = ch;
	    }
	    if (arg.length() > 0) {
		args.add(arg.toString());
	    }
	    String[] argv = new String[args.size()];
	    argv = args.toArray(argv);
	    p = Runtime.getRuntime().exec(argv, null, cwd);
	}

	public InputStream getInputStream() throws IOException {
	    if (debug) {
		if (debugIn == null) {
		    File f = null;
		    if (wsdir == null) {
			f = new File("out." + pid + ".log");
		    } else {
			f = new File(wsdir, "out." + pid + ".log");
		    }
		    debugIn = new StreamLogger(command, p.getInputStream(), f, logger);
		}
		return debugIn;
	    } else {
		return p.getInputStream();
	    }
	}

	public InputStream getErrorStream() throws IOException {
	    if (debug) {
		if (debugErr == null) {
		    File f = null;
		    if (wsdir == null) {
			f = new File("err." + pid + ".log");
		    } else {
			f = new File(wsdir, "err." + pid + ".log");
		    }
		    debugErr = new StreamLogger(command, p.getErrorStream(), f, logger);
		}
		return debugErr;
	    } else {
		return p.getErrorStream();
	    }
	}

	public OutputStream getOutputStream() {
	    return p.getOutputStream();
	}

	public void waitFor(long millis) throws InterruptedException {
	    if (millis == 0) {
		p.waitFor();
	    } else {
		TimerTask task = new InterruptTask(Thread.currentThread());
		long expires = System.currentTimeMillis() + millis;
		JOVALSystem.getTimer().schedule(task, new Date(expires));
		InterruptedException ie = null;
		try {
		    p.waitFor();
		} catch (InterruptedException e) {
		    ie = e;
		}
		if (task.cancel()) {
		    JOVALSystem.getTimer().purge();
		    if (ie != null) {
			throw ie;
		    }
		}
	    }
	}

	public int exitValue() throws IllegalThreadStateException {
	    return p.exitValue();
	}

	public void destroy() {
	    p.destroy();
	}

	public boolean isRunning() {
	    try {
		exitValue();
		return false;
	    } catch (IllegalThreadStateException e) {
		return true;
	    }
	}
    }

    class InterruptTask extends TimerTask {
	Thread t;

	InterruptTask(Thread t) {
	    this.t = t;
	}

	public void run() {
	    if (t.isAlive()) {
		t.interrupt();
	    }
	}
    }
}
