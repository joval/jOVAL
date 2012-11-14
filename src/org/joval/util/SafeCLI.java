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

import org.joval.intf.io.IReader;
import org.joval.intf.io.IReaderGobbler;
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
     * Run a command and get the first (non-empty) line of output.
     */
    public static final String exec(String cmd, IBaseSession session, IBaseSession.Timeout to) throws Exception {
	return exec(cmd, null, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the first (non-empty) line of output, using the specified environment.
     */
    public static final String exec(String cmd, String[] env, IBaseSession session, IBaseSession.Timeout to) throws Exception {
	return exec(cmd, env, session, session.getTimeout(to));
    }

    /**
     * Run a command and get the first (non-empty) line of output.
     */
    public static final String exec(String cmd, IBaseSession session, long readTimeout) throws Exception {
	return exec(cmd, null, session, readTimeout);
    }

    /**
     * Run a command and get the first (non-empty) line of output, using the specified environment.
     */
    public static final String exec(String cmd, String[] env, IBaseSession session, long readTimeout) throws Exception {
	List<String> lines = multiLine(cmd, env, session, readTimeout);
	if (lines.size() == 2 && lines.get(0).equals("")) {
	    return lines.get(1);
	} else {
	    return lines.get(0);
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

	return execData(cmd, env, session, readTimeout).getLines();
    }

    /**
     * Run a command and get the resulting ExecData, using the specified environment.
     */
    public static final ExecData execData(String cmd, String[] env, IBaseSession session, long readTimeout)
		throws Exception {

	SafeCLI cli = new SafeCLI(cmd, env, session, readTimeout);
	cli.exec(session.echo());
	return cli.getResult();
    }

    /**
     * Run a command, using the specified output processor ("gobbler").
     *
     * When the command is run, an IReader to the output is passed to the gobbler using the gobble method. If the command
     * hangs (signaled to SafeCLI by an InterruptedIOException or SessionException), the SafeCLI will kill the old command,
     * start a new command instance (up the the session's configured number of retries), and call gobbler.gobble again.
     *
     * Hence, the gobbler should initialize itself completely when gobble is invoked, and not perform permanent output
     * processing until the reader has reached the end of the process output.
     */
    public static final void exec(String cmd, String[] env, IBaseSession session, long readTimeout,
				  IReaderGobbler out, IReaderGobbler err) throws Exception {

	new SafeCLI(cmd, env, session, readTimeout).exec(out, err);
    }

    /**
     * A container for information resulting from the execution of a process.
     */
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

	/**
	 * Guaranteed to have at least one entry.
	 */
	public List<String> getLines() throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
	    List<String> lines = new Vector<String>();
	    String line = null;
	    while((line = reader.readLine()) != null) {
		lines.add(line);
	    }
	    if (lines.size() == 0) {
		session.getLogger().warn(JOVALMsg.WARNING_MISSING_OUTPUT, cmd, exitCode, data.length);
		lines.add("");
	    }
	    return lines;
	}
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
	result = new ExecData();
    }

    private ExecData getResult() {
	return result;
    }

    private void exec(IReaderGobbler outputGobbler, IReaderGobbler errorGobbler) throws Exception {
	boolean success = false;
	for (int attempt=0; !success; attempt++) {
	    IProcess p = null;
	    PerishableReader reader = null;
	    try {
		p = session.createProcess(cmd, env);
		p.start();
		reader = PerishableReader.newInstance(p.getInputStream(), readTimeout);
		reader.setLogger(session.getLogger());
		if (errorGobbler != null) {
		    new GobblerThread(errorGobbler).start(PerishableReader.newInstance(p.getErrorStream(), readTimeout));
		}
		outputGobbler.gobble(reader);
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
	    } catch (SessionException e) {
		if (attempt > execRetries) {
		    session.getLogger().warn(JOVALMsg.ERROR_PROCESS_RETRY, cmd, attempt);
		    throw e;
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_SESSION_INTEGRITY, e.getMessage());
		    session.getLogger().info(JOVALMsg.STATUS_PROCESS_RETRY, cmd);
		    session.disconnect();
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

    private void exec(boolean skipFirstLine) throws Exception {
	exec(new InnerGobbler(skipFirstLine), null);
    }

    /**
     * An IReaderGobbler that reads data into an ExecData. This internal implementation sets the ExecData result for the
     * class.
     */
    class InnerGobbler implements IReaderGobbler {
	//
	// For sessions that ignore the terminal mode bytes and echo anyway, discard the first line
	// of output, as it's always the echo of the issued command.
	//
	private boolean skipFirstLine;

	InnerGobbler(boolean skipFirstLine) {
	    this.skipFirstLine = skipFirstLine;
	}

	public void gobble(IReader reader) throws IOException {
	    if (skipFirstLine) {
		reader.readLine();
	    }
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    byte[] buff = new byte[512];
	    int len = 0;
	    while((len = reader.getStream().read(buff)) > 0) {
		out.write(buff, 0, len);
	    }
	    result.data = out.toByteArray();
	}
    }

    class GobblerThread implements Runnable {
	Thread thread;
	IReader reader;
	IReaderGobbler gobbler;

	GobblerThread(IReaderGobbler gobbler) {
	    this.gobbler = gobbler;
	}

	void start(IReader reader) throws IllegalStateException {
	    if (thread == null || !thread.isAlive()) {
		this.reader = reader;
		thread = new Thread(this);
		thread.start();
	    } else {
		throw new IllegalStateException("running");
	    }
	}

	// Implement Runnable

	public void run() {
	    try {
		gobbler.gobble(reader);
	    } catch (IOException e) {
	    }
	}
    }
}
