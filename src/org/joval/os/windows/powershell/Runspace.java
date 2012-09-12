// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.joval.intf.system.IProcess;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.io.PerishableReader;
import org.joval.util.StringTools;

/**
 * A process-based implementation of an IRunspace.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Runspace implements IRunspace {
    protected String id, prompt;
    protected IProcess p;
    protected InputStream stdout, stderr;	// Output from the powershell process
    protected OutputStream stdin;		// Input to the powershell process

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

    public void loadModule(InputStream in, long timeout) throws IOException {
	try {
	    StringBuffer buffer = new StringBuffer();
	    String line = null;
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StringTools.ASCII));
	    while((line = reader.readLine()) != null) {
		line = line.trim();
		if (!line.startsWith("#") && line.length() > 0) {
		    stdin.write(line.getBytes());
		    stdin.write("\r\n".getBytes());
		    stdin.flush();
		    readLine(timeout);
		}
	    }
	    if (">> ".equals(getPrompt())) {
		invoke("");
		readLine(timeout);
	    }
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
	if (hasError()) {
	    throw new IOException(getError());
	}
    }

    public void invoke(String command) throws IOException {
	byte[] bytes = command.trim().getBytes();
	stdin.write(bytes);
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

    public boolean hasError() {
	try {
	    return p.getErrorStream().available() > 0;
	} catch (IOException e) {
	    return true;
	}
    }

    public String getError() throws IOException {
	byte[] buff = new byte[p.getErrorStream().available()];
	p.getErrorStream().read(buff);
	return new String(buff, StringTools.ASCII);
    }

    public String getPrompt() {
	return prompt;
    }

    // Internal

    protected boolean isPrompt(String str) {
	return (str.startsWith("PS") && str.endsWith("> ")) || str.equals(">> ");
    }
}
