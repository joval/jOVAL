// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.FamilyEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;

/**
 * Base class for the Windows and Unix local ISession implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseSession implements ISession {
    protected LocLogger logger;
    protected File cwd;
    protected IEnvironment env;
    protected IFilesystem fs;
    protected boolean debug = false;

    protected BaseSession() {
	logger = JOVALSystem.getLogger();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement ISession

    public void setDebug(boolean debug) {
	this.debug = debug;
    }

    public void setWorkingDir(String path) {
	cwd = new File(path);
    }

    public IEnvironment getEnvironment() {
	return env;
    }

    public IFilesystem getFilesystem() {
	return fs;
    }

    public IProcess createProcess(String command) throws Exception {
	return new JavaProcess(command);
    }

    public FamilyEnumeration getFamily() {
	switch(getType()) {
	  case WINDOWS:
	    return FamilyEnumeration.WINDOWS;

	  case UNIX:
	    switch(((IUnixSession)this).getFlavor()) {
	      case MACOSX:
		return FamilyEnumeration.MACOS;
	      default:
		return FamilyEnumeration.UNIX;
	    }

	  case CISCO_IOS:
	    return FamilyEnumeration.IOS;

	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }

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
