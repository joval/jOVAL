// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.joval.intf.system.IProcess;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.io.PerishableReader;
import org.joval.util.SessionException;

/**
 * A process-based implementation of an IRunspace.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Runspace implements IRunspace {
    private String id, prompt;
    private IProcess p;
    private InputStream stdout, stderr;	// Output from the powershell process
    private OutputStream stdin;		// Input to the powershell process

    /**
     * Create a new Runspace, based on a process.
     */
    public Runspace(String id, IProcess p) throws Exception {
	this.id = id;
	this.p = p;
	if (!p.isRunning()) {
	    p.start();
	}
	stdout = p.getInputStream();
	stderr = p.getErrorStream();
	stdin = p.getOutputStream();
	read(10000L);
    }

    public IProcess getProcess() {
	return p;
    }

    // Implement IRunspace

    public String getId() {
	return id;
    }

    public void println(String str) throws IOException {
	stdin.write(str.getBytes());
	stdin.write("\r\n".getBytes());
	stdin.flush();
    }

    public synchronized String read(long timeout) throws IOException {
	StringBuffer sb = null;
	String line = null;
	while((line = readLine(timeout)) != null) {
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

    public synchronized String readLine(long timeout) throws IOException {
	PerishableReader reader = PerishableReader.newInstance(stdout, timeout);
	try {
	    StringBuffer sb = new StringBuffer();
	    boolean cr = false;
	    int ch = -1;
	    while((ch = reader.read()) != -1) {
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
		if (stdout.available() == 0 && isPrompt(sb.toString())) {
		    prompt = sb.toString();
		    return null;
		}
	    }
	} finally {
	    reader.defuse();
	}
	return null;
    }

    /**
     * Get the current prompt String.
     */
    public String getPrompt() {
	return prompt;
    }

    // Private

    private boolean isPrompt(String str) {
	return str.startsWith("PS") && str.endsWith("> ");
    }
}
