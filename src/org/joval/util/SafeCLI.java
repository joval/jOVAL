// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Vector;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.io.PerishableReader;
import org.joval.util.JOVALMsg;

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
    public static final String exec(String cmd, IBaseSession session, IBaseSession.Timeout to) throws Exception {
	return exec(cmd, null, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the first line of output, using the specified environment.
     */
    public static final String exec(String cmd, String[] env, IBaseSession session, IBaseSession.Timeout to) throws Exception {
	return exec(cmd, env, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the first line of output.
     */
    public static final String exec(String cmd, IBaseSession session, long readTimeout) throws Exception {
	return exec(cmd, null, session, readTimeout);
    }

    /**
     * Run a command and get the first line of output, using the specified environment.
     */
    public static final String exec(String cmd, String[] env, IBaseSession session, long readTimeout) throws Exception {
	List<String> lines = multiLine(cmd, env, session, readTimeout);
	if (lines != null && lines.size() > 0) {
	    return lines.get(0);
	} else {
	    return null;
	}
    }

    /**
     * Run a command and get the resulting lines of output.
     */
    public static final List<String> multiLine(String cmd, IBaseSession session, IBaseSession.Timeout to) throws Exception {
	return multiLine(cmd, null, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the resulting lines of output, using the specified environment.
     */
    public static final List<String> multiLine(String cmd, String[] env, IBaseSession session, IBaseSession.Timeout to)
		throws Exception {

	return multiLine(cmd, env, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the resulting lines of output.
     */
    public static final List<String> multiLine(String cmd, IBaseSession session, long readTimeout) throws Exception {
	return multiLine(cmd, null, session, readTimeout);
    }

    /**
     * Run a command and get the resulting lines of output, using the specified environment.
     */
    public static final List<String> multiLine(String cmd, String[] env, IBaseSession session, long readTimeout)
		throws Exception {

	byte[] buff = execData(cmd, env, session, readTimeout).data;
	BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buff)));
	List<String> lines = new Vector<String>();
	String line = null;
	while((line = reader.readLine()) != null) {
	    lines.add(line);
	}
	return lines;
    }

    /**
     * Run a command and get the resulting ExecData, using the specified environment.
     */
    public static final ExecData execData(String cmd, String[] env, IBaseSession session, long readTimeout)
		throws Exception {

	return new SafeCLI(cmd, env, session, readTimeout).result();
    }

    // Private

    private String cmd;
    private String[] env;
    private IBaseSession session;
    private ExecData result;
    private long readTimeout;
    private int execRetries = 0;

    private SafeCLI(String cmd, String[] env, IBaseSession session, long readTimeout) throws Exception {
	this.cmd = cmd;
	this.env = env;
	this.session = session;
	this.readTimeout = readTimeout;
	execRetries = session.getProperties().getIntProperty(IBaseSession.PROP_EXEC_RETRIES);
    }

    private ExecData result() throws Exception {
	if (result == null) {
	    exec();
	}
	return result;
    }

    public void exec() throws Exception {
	boolean success = false;
	result = new ExecData();

	for (int attempt=0; !success; attempt++) {
	    IProcess p = null;
	    PerishableReader reader = null;
	    try {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		p = session.createProcess(cmd, env);
		p.start();
		reader = PerishableReader.newInstance(p.getInputStream(), readTimeout);
		reader.setLogger(session.getLogger());
		byte[] buff = new byte[512];
		int len = 0;
		while((len = reader.read(buff)) > 0) {
		    out.write(buff, 0, len);
		}
		result.data = out.toByteArray();
		try {
		    p.waitFor(session.getTimeout(IBaseSession.Timeout.M));
		    result.exitCode = p.exitValue();
		    success = true;
		} catch (InterruptedException e) {
		}
	    } catch (IOException e) {
		if (e instanceof InterruptedIOException || e instanceof EOFException) {
		    if (attempt > execRetries) {
			session.getLogger().warn(JOVALMsg.ERROR_PROCESS_RETRY, cmd, attempt);
			throw e;
		    } else {
			// the process has hung up, so kill it
			p.destroy();
			p = null;
			session.getLogger().info(JOVALMsg.STATUS_PROCESS_RETRY, cmd);
		    }
		} else {
		    throw e;
		}
	    } finally {
		if (reader != null) {
		    try {
			reader.close();
		    } catch (IOException e) {
		    }
		}
		if (p != null) {
		    try {
			InputStream err = p.getErrorStream();
			if (err != null) {
			    err.close();
			}
		    } catch (IOException e) {
		    }
		    if (p.isRunning()) {
			p.destroy();
		    }
		}
	    }
	}
    }

    public class ExecData {
	int exitCode;
	byte[] data;

	ExecData() {
	    exitCode = -1;
	    data = null;
	}

	public int getExitCode() {
	    return exitCode;
	}

	public byte[] getData() {
	    return data;
	}
    }
}
