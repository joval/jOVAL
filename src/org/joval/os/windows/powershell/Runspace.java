// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IProcess;
import org.joval.intf.windows.powershell.IRunspace;
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

    protected String id, prompt;
    protected StringBuffer err;
    protected LocLogger logger;
    protected IProcess p;
    protected InputStream stdout, stderr;	// Output from the powershell process
    protected OutputStream stdin;		// Input to the powershell process

    /**
     * Create a new Runspace, based on a process.
     */
    public Runspace(String id, IProcess p, LocLogger logger) throws Exception {
	this.id = id;
	this.logger = logger;
	this.p = p;
	if (!p.isRunning()) {
	    p.start();
	}
	stdout = p.getInputStream();
	stderr = p.getErrorStream();
	stdin = p.getOutputStream();
	err = null;
	read();
    }

    public IProcess getProcess() {
	return p;
    }

    // Implement IRunspace

    public String getId() {
	return id;
    }

    public synchronized void loadModule(InputStream in) throws IOException, PowershellException {
	try {
	    StringBuffer buffer = new StringBuffer();
	    String line = null;
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StringTools.ASCII));
	    while ((line = reader.readLine()) != null) {
		stdin.write(line.getBytes());
		stdin.write("\r\n".getBytes());
		stdin.flush();
		readLine();
	    }
	    if (">> ".equals(getPrompt())) {
		invoke("");
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
	logger.debug(JOVALMsg.STATUS_POWERSHELL_INVOKE, id, command);
	byte[] bytes = command.trim().getBytes();
	stdin.write(bytes);
	stdin.write("\r\n".getBytes());
	stdin.flush();
	String result = read();
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

    // Internal

    /**
     * Read lines until the next prompt is reached. If there are errors, they are buffered in err.
     */
    protected String read() throws IOException {
	StringBuffer sb = null;
	String line = null;
	while((line = readLine()) != null) {
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
    protected String readLine() throws IOException {
	StringBuffer sb = new StringBuffer();
	boolean cr = false;
	int ch = -1;
	if (hasError()) {
	    throw new IOException(getError());
	}
	while((ch = stdout.read()) != -1) {
	    switch(ch) {
	      case '\r':
		cr = true;
		if (stdout.markSupported() && stdout.available() > 0) {
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
	    if (hasError()) {
		if (err == null) {
		    err = new StringBuffer(getError());
		} else {
		    err.append("\r\n");
		    err.append(getError());
		}
	    } else if (stdout.available() == 0 && isPrompt(sb.toString())) {
		prompt = sb.toString();
		return null;
	    }
	}
	return null;
    }

    /**
     * Is there data available in the error stream?
     */
    protected boolean hasError() {
	try {
	    return p.getErrorStream().available() > 0;
	} catch (IOException e) {
	    return true;
	}
    }

    /**
     * Get all the data available in the error stream.
     */
    protected String getError() throws IOException {
	byte[] buff = new byte[p.getErrorStream().available()];
	p.getErrorStream().read(buff);
	return new String(buff, StringTools.ASCII);
    }

    protected boolean isPrompt(String str) {
	return (str.startsWith("PS") && str.endsWith("> ")) || str.equals(">> ");
    }
}
