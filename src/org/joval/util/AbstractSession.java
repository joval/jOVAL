// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;

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

    protected AbstractSession() {
	super();
    }

    // Implement ILoggable

    /**
     * @override
     *
     * Here, we harmonize the IFilesystem's logger with the ISession's logger.
     */
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
     * @override
     *
     * Here, we provide an implementation for local ISessions.
     */
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

    class JavaProcess implements IProcess {
	String command;
	Process p;

	JavaProcess(String command) {
	    this.command = command;
	}

	// Implement IProcess

	public void setInteractive(boolean interactive) {
	}

	public void start() throws Exception {
	    p = Runtime.getRuntime().exec(command, null, cwd);
	}

	public InputStream getInputStream() {
	    return p.getInputStream();
	}

	public InputStream getErrorStream() {
	    return p.getErrorStream();
	}

	public OutputStream getOutputStream() {
	    return p.getOutputStream();
	}

	public void waitFor(long millis) throws InterruptedException {
	    new Timer(millis, new TimerClient(Thread.currentThread()));
	    p.waitFor();
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

    class Timer implements Runnable {
	private long millis;
	private TimerClient client;
	private Thread t;

	Timer(long millis, TimerClient client) {
	    this.millis = millis;
	    this.client = client;
	    t = new Thread(this);
	}

	void start() {
	    t.start();
	}

	void cancel() {
	    if (t.isAlive()) {
		t.interrupt();
	    }
	}

	// Implement Runnable

	public void run() {
	    try {
		Thread.sleep(millis);
		client.ding();
	    } catch (InterruptedException e) {
	    }
	}
    }

    class TimerClient {
	Thread t;

	TimerClient (Thread t) {
	    this.t = t;
	}

	void ding() {
	    if (t.isAlive()) {
		t.interrupt();
	    }
	}
    }
}
