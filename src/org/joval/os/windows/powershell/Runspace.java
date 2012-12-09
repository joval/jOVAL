// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.HashSet;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IProcess;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.io.StreamLogger;
import org.joval.util.Checksum;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * A process-based implementation of an IRunspace.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Runspace implements IRunspace {
    public static final String INIT_COMMAND = "powershell -NoProfile -File -";

    private long timeout;		// contains default timeout
    private String id, prompt;
    private StringBuffer err;
    private LocLogger logger;
    private IWindowsSession.View view;
    private IProcess p;
    private InputStream stdout, stderr;	// Output from the powershell process
    private OutputStream stdin;		// Input to the powershell process
    private HashSet<String> modules;
    private Charset encoding = null;

    /**
     * Create a new Runspace, based on the default architecture.
     */
    public Runspace(String id, IWindowsSession session) throws Exception {
	this(id, session, session.supports(IWindowsSession.View._64BIT) ? IWindowsSession.View._64BIT : null);
    }

    /**
     * Create a new Runspace, using the specified architecture (null for default).
     */
    public Runspace(String id, IWindowsSession session, IWindowsSession.View view) throws Exception {
	this(id, session, view, StringTools.ASCII);
    }

    /**
     * Create a new Runspace, using the specified architecture (null for default) and encoding.
     */
    public Runspace(String id, IWindowsSession session, IWindowsSession.View view, Charset encoding) throws Exception {
	this.id = id;
	this.timeout = session.getTimeout(IWindowsSession.Timeout.M);
	this.logger = session.getLogger();
	this.view = view;
	this.encoding = encoding;
	modules = new HashSet<String>();
	if (view == IWindowsSession.View._32BIT) {
	    String cmd = new StringBuffer("%SystemRoot%\\SysWOW64\\cmd.exe /c ").append(INIT_COMMAND).toString();
	    p = session.createProcess(cmd, null);
	} else {
	    p = session.createProcess(INIT_COMMAND, null);
	}
	p.start();
	stdout = p.getInputStream();
	stderr = p.getErrorStream();
	stdin = p.getOutputStream();
	err = null;
	readBOM();
	read(timeout);
    }

    public IProcess getProcess() {
	return p;
    }

    // Implement IRunspace

    public String getId() {
	return id;
    }

    public void loadModule(InputStream in) throws IOException, PowershellException {
	loadModule(in, timeout);
    }

    public synchronized void loadModule(InputStream in, long millis) throws IOException, PowershellException {
	try {
	    ByteArrayOutputStream buff = new ByteArrayOutputStream();
	    StreamLogger input = new StreamLogger(null, in, buff);
	    String cs = Checksum.getChecksum(input, Checksum.Algorithm.MD5);
	    input.close();
	    in = null;
	    if (modules.contains(cs)) {
		logger.debug(JOVALMsg.STATUS_POWERSHELL_MODULE_SKIP, cs);
	    } else {
		logger.debug(JOVALMsg.STATUS_POWERSHELL_MODULE_LOAD, cs);
		in = new ByteArrayInputStream(buff.toByteArray());
		String line = null;
		int lines = 0;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding));
		while((line = reader.readLine()) != null) {
		    stdin.write(line.getBytes());
		    stdin.write("\r\n".getBytes());
		    lines++;
		}
		stdin.flush();
		for (int i=0; i < lines; i++) {
		    readPrompt(millis);
		}
		if (">> ".equals(getPrompt())) {
		    invoke("");
		}
		// DAS: add only if there was no error?
		modules.add(cs);
	    }
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
	if (err != null) {
	    String error = err.toString();
	    err = null;
	    throw new PowershellException(error);
	}
    }

    public synchronized String invoke(String command) throws IOException, PowershellException {
	return invoke(command, timeout);
    }

    public synchronized String invoke(String command, long millis) throws IOException, PowershellException {
	logger.debug(JOVALMsg.STATUS_POWERSHELL_INVOKE, id, command);
	byte[] bytes = command.trim().getBytes();
	stdin.write(bytes);
	stdin.write("\r\n".getBytes());
	stdin.flush();
	String result = read(millis);
	if (err == null) {
	    return result;
	} else {
	    String error = err.toString();
	    err = null;
	    throw new PowershellException(error);
	}
    }

    public String getPrompt() {
	return prompt;
    }

    public IWindowsSession.View getView() {
	return view;
    }

    // Internal

    /**
     * Read lines until the next prompt is reached. If there are errors, they are buffered in err.
     */
    protected String read(long millis) throws IOException {
	StringBuffer sb = null;
	String line = null;
	while((line = readLine(millis)) != null) {
	    if (sb == null) {
		sb = new StringBuffer();
	    } else {
		sb.append("\r\n");
	    }
	    sb.append(line);
	}
	if (sb == null) {
	    return null;
	} else {
	    return sb.toString();
	}
    }

    /**
     * Read a single line, or the next prompt. Returns null if the line is a prompt. If there are errors, they are
     * buffered in err.
     */
    private String readLine(long millis) throws IOException {
	StringBuffer sb = new StringBuffer();
	//
	// Poll the streams for no more than timeout millis if there is no data.
	//
	int interval = 25;
	int max_iterations = (int)(millis / interval);
	for (int i=0; i < max_iterations; i++) {
	    int avail = 0;
	    if ((avail = stderr.available()) > 0) {
		if (err == null) {
		    err = new StringBuffer();
		}
		byte[] buff = new byte[avail];
		stderr.read(buff);
		err.append(new String(buff, encoding));
	    }
	    if ((avail = stdout.available()) > 0) {
		boolean cr = false;
		while(avail-- > 0) {
		    int ch = stdout.read();
		    switch(ch) {
		      case '\r':
			cr = true;
			if (stdout.markSupported() && avail > 0) {
			    stdout.mark(1);
			    switch(stdout.read()) {
			      case '\n':
				return sb.toString();
			      default:
				stdout.reset();
				break;
			    }
			}
			break;

		      case '\n':
			return sb.toString();

		      default:
			if (cr) {
			    cr = false;
			    sb.append((char)('\r' & 0xFF));
			}
			sb.append((char)(ch & 0xFF));
		    }
		}
		if (isPrompt(sb.toString())) {
		    prompt = sb.toString();
		    return null;
		}
		i = 0; // reset the I/O timeout counter
	    }
	    if (p.isRunning()) {
		try {
		    Thread.sleep(interval);
		} catch (InterruptedException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    throw new IOException(e);
		}
	    } else {
		if (sb.length() > 0) {
		    return sb.toString();
		} else {
		    return null;
		}
	    }
	}
	throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL_TIMEOUT));
    }

    /**
     * Read a prompt. There must be NO other output to stdout, or this call will time out. Error data is buffered to err.
     */
    private synchronized void readPrompt(long millis) throws IOException {
	StringBuffer sb = new StringBuffer();
	//
	// Poll the streams for no more than timeout millis if there is no data.
	//
	int interval = 250;
	int max_iterations = (int)(millis / interval);
	for (int i=0; i < max_iterations; i++) {
	    int avail = 0;
	    if ((avail = stderr.available()) > 0) {
		if (err == null) {
		    err = new StringBuffer();
		}
		byte[] buff = new byte[avail];
		stderr.read(buff);
		err.append(new String(buff, encoding));
	    }
	    if ((avail = stdout.available()) > 0) {
		boolean cr = false;
		while(avail-- > 0) {
		    sb.append((char)(stdout.read() & 0xFF));
		    if (isPrompt(sb.toString())) {
			prompt = sb.toString();
			return;
		    }
		}
		i = 0; // reset the I/O timeout counter
	    }
	    if (p.isRunning()) {
		try {
		    Thread.sleep(interval);
		} catch (InterruptedException e) {
		    throw new IOException(e);
		}
	    }
	}
	throw new InterruptedIOException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL_TIMEOUT));
    }

    private boolean isPrompt(String str) {
	return (str.startsWith("PS") && str.endsWith("> ")) || str.equals(">> ");
    }

    private void readBOM() throws IOException {
	if (encoding == StringTools.UTF8) {
	    // EE BB BF
	    stdout.read();
	    stdout.read();
	    stdout.read();
	} else if (encoding == StringTools.UTF16 || encoding == StringTools.UTF16LE) {
	    // FE FF (big) or FF FE (little)
	    stdout.read();
	    stdout.read();
	}
    }
}
