// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamLogger;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;

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
    protected HashSet<String> toDelete;

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

    public void deleteOnDisconnect(IFile file) throws IllegalStateException {
	if (connected) {
	    if (toDelete == null) {
		toDelete = new HashSet<String>();
	    }
	    toDelete.add(file.getPath());
	} else {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_NOT_CONNECTED));
	}
    }

    public String getTempDir() throws IOException {
	return System.getProperty("java.io.tmpdir");
    }

    public IEnvironment getEnvironment() {
	return env;
    }

    public IFilesystem getFilesystem() {
	return fs;
    }

    // Implement IBaseSession

    @Override
    public void dispose() {
	super.dispose();
	if (fs instanceof CachingHierarchy) {
	    ((CachingHierarchy)fs).dispose();
	}
    }

    /**
     * The account name running the Java process.
     */
    @Override
    public String getUsername() {
	return System.getProperty("user.name");
    }

    @Override
    public long getTime() throws Exception {
	return System.currentTimeMillis();
    }

    /**
     * Here, we provide an implementation for local ISessions.
     */
    @Override
    public IProcess createProcess(String command, String[] env) throws Exception {
	return new JavaProcess(command, env);
    }

    // All the abstract methods, for reference

    public abstract boolean connect();

    public abstract void disconnect();

    public abstract String getHostname();

    public abstract Type getType();

    // Internal

    /**
     * Delete all the files that have been registered for deletion on disconnect.  Subclasses should call this.
     */
    protected void deleteFiles() {
	for (String fname : toDelete) {
	    try {
		IFile f = fs.getFile(fname, IFile.READWRITE);
		if (f.isFile()) {
		    f.delete();
		}
	    } catch (IOException e) {
		logger.warn(JOVALMsg.ERROR_FILE_DELETE, fname);
	    }
	}
    }

    private static final char NULL = (char)0;
    private static final char SQ = '\'';
    private static final char DQ = '\"';
    private static final char ESC = '\\';

    private int pid = 1;

    class JavaProcess implements IProcess {
	String command;
	String[] env;
	Process p;
	int pid;
	StreamLogger debugIn, debugErr;

	JavaProcess(String command, String[] env) {
	    this.command = command;
	    this.env = env;
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
	 * simplistic, we invoke the native shell to interpret the command string.
	 */
	public void start() throws Exception {
	    List<String>args = new Vector<String>();
	    if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
		args.add("/bin/sh");
		args.add("-c");
		args.add(command);
	    } else {
		args.add("cmd");
		args.add("/c");
		args.add(command);
	    }
	    try {
		ProcessBuilder pb = new ProcessBuilder(args);
		if (env != null) {
		    for (String s : env) {
			int ptr = s.indexOf("=");
			if (ptr > 0) {
			    pb.environment().put(s.substring(0, ptr), s.substring(ptr+1));
			}
		    }
		}
		p = pb.directory(cwd).start();
	    } catch (IOException e) {
		throw new Exception(e);
	    }
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
		    debugIn = new StreamLogger(command, p.getInputStream(), f);
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
		    debugErr = new StreamLogger(command, p.getErrorStream(), f);
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
