// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.powershell;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.joval.intf.system.IProcess;
import org.joval.io.PerishableReader;
import org.joval.util.StringTools;

/**
 * A Runspace implementation with a flush-optimized implementation of loadModule, for use with MS-WSMV.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Runspace extends org.joval.os.windows.powershell.Runspace {
    /**
     * Create a new Runspace, based on a process.
     */
    Runspace(String id, IProcess p) throws Exception {
	super(id, p);
    }

    // Implement IRunspace

    /**
     * For efficient use of WS-Transfer/Send, this method transmits the entire module in one envelope, then loops
     * through the resulting string of prompts.
     */
    @Override
    public void loadModule(InputStream in, long timeout) throws IOException {
	try {
	    StringBuffer buffer = new StringBuffer();
	    String line = null;
	    int lines = 0;
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StringTools.ASCII));
	    while((line = reader.readLine()) != null) {
		line = line.trim();
		if (!line.startsWith("#") && line.length() > 0) {
		    stdin.write(line.getBytes());
		    stdin.write("\r\n".getBytes());
		    lines++;
		}
	    }
	    stdin.flush();
	    for (int i=0; i < lines; i++) {
		readLineInternal(timeout);
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
    }

    // Private

    /**
     * Same as super.readLine(long), except insensitive to stdout.available().  This makes it usable with input that
     * has built-up from a multi-line WS-Transfer/Send operation continaing input to the process.
     */
    private synchronized String readLineInternal(long timeout) throws IOException {
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
		if (isPrompt(sb.toString())) {
		    prompt = sb.toString();
		    return null;
		}
	    }
	} finally {
	    reader.defuse();
	}
	return null;
    }
}
