// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Vector;

import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.PerishableReader;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A tool for attempting to run a command-line repeatedly until it spits out some results.  It can only be used for commands
 * that require no input from stdin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SafeCLI {
    /**
     * Run a command and get the first line of output.
     */
    public static final String exec(String cmd, IBaseSession session, long readTimeout) throws Exception {
	SafeCLI sc = new SafeCLI(cmd, session, readTimeout);
	List<String> output = sc.output();
	if (output != null && output.size() > 0) {
	    return output.get(0);
	} else {
	    return null;
	}
    }

    /**
     * Run a command and get the resulting lines of output.
     */
    public static final List<String> multiLine(String cmd, IBaseSession session, long readTimeout) throws Exception {
	return new SafeCLI(cmd, session, readTimeout).output();
    }

    // Private

    private String cmd;
    private IBaseSession session;
    private List<String> output;
    private long readTimeout;
    private int execRetries = 0;

    private SafeCLI(String cmd, IBaseSession session, long readTimeout) throws Exception {
	this.cmd = cmd;
	this.session = session;
	this.readTimeout = readTimeout;

	//
	// Avoid calling SshSession.getType(), as that would result in an infinite loop!
	//
	if (session instanceof ISession) {
	    IBaseSession.Type type = session.getType();
	    switch(type) {
	      case UNIX:
		execRetries = JOVALSystem.getIntProperty(JOVALSystem.PROP_UNIX_EXEC_RETRIES);
		break;

	      case WINDOWS:
		execRetries = JOVALSystem.getIntProperty(JOVALSystem.PROP_WINDOWS_EXEC_RETRIES);
		break;

	      default:
		session.getLogger().warn(JOVALMsg.ERROR_SESSION_TYPE, type);
		break;
	    }
	} else {
	    execRetries = JOVALSystem.getIntProperty(JOVALSystem.PROP_SSH_EXEC_RETRIES);
	}
    }

    private List<String> output() throws Exception {
	if (output == null) {
	    exec();
	}
	return output;
    }

    public void exec() throws Exception {
	boolean success = false;

	for (int attempt=0; !success; attempt++) {
	    IProcess p = null;
	    IReader in = null;
	    try {
		p = session.createProcess(cmd);
		p.start();
		in = PerishableReader.newInstance(p.getInputStream(), readTimeout);
		in.setLogger(session.getLogger());
		output = new Vector<String>();
		String line = null;
		while((line = in.readLine()) != null) {
		    output.add(line);
		}
		success = true;
	    } catch (IOException e) {
		if (e instanceof InterruptedIOException || e instanceof EOFException) {
		    if (attempt > execRetries) {
			session.getLogger().warn(JOVALMsg.ERROR_PROCESS_RETRY, cmd, attempt);
			throw e;
		    } else {
			// Something's probably wrong with the connection, so reconnect it.
			session.disconnect();
			session.connect();
			session.getLogger().info(JOVALMsg.STATUS_PROCESS_RETRY, cmd);
		    }
		} else {
		    throw e;
		}
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
		    }
		}
		if (p != null) {
		    try {
			p.waitFor(0);
		    } catch (InterruptedException e) {
		    }
		}
	    }
	}
    }
}
