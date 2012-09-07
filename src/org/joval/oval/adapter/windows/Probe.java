// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.io.IFile;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Provides utility methods for using a host-based executable probe.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class Probe {
    private IWindowsSession session;
    private String id;
    private boolean installed;
    private String installPath;
    private long timeout;
    private String[] env;

    /**
     * Initialize the adapter and install the probe on the target host.
     *
     * @param session the IWindowsSession for the target machine
     * @param probeId the resource identifier for the executable probe
     */
    Probe(IWindowsSession session, String probeId) {
	this.id = probeId;
	this.session = session;
	timeout = session.getTimeout(IBaseSession.Timeout.M);
	env = session.getEnvironment().toArray();
	installed = false;
    }

    /**
     * Install the probe.
     */
    boolean install() {
	try {
	    StringBuffer sb = new StringBuffer(session.getTempDir());
	    sb.append(IWindowsFilesystem.DELIM_STR).append(id);
	    IFile exe = session.getFilesystem().getFile(sb.toString(), IFile.READWRITE);
	    byte[] buff = new byte[1024];
	    InputStream in = AccesstokenAdapter.class.getResourceAsStream(id);
	    if (in == null) {
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, id));
	    }
	    OutputStream out = exe.getOutputStream(false);
	    try {
		int len = 0;
		while ((len = in.read(buff)) > 0) {
		    out.write(buff, 0, len);
		}
		installPath = exe.getPath();
		session.deleteOnDisconnect(exe);
		installed = true;
	    } catch (IOException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_IO), exe.toString(), e.getMessage());
	    } finally {
		if (out != null) {
		    out.close();
		}
		if (in != null) {
		    in.close();
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return installed;
    }

    /**
     * Check whether or not the probe has been installed.
     */
    boolean isInstalled() {
	return installed;
    }

    /**
     * Execute the probe with the specified arguments.
     */
    SafeCLI.ExecData exec(String[] args) throws Exception {
	String path = installPath;
	if (path.indexOf(" ") != -1) {
	    path = quote(path);
	}
	StringBuffer sb = new StringBuffer(path);
	for (String arg : args) {
	    sb.append(" ");
	    if (arg.indexOf(" ") == -1) {
		sb.append(arg);
	    } else {
		sb.append(quote(arg));
	    }
	}
	return SafeCLI.execData(sb.toString(), env, session, timeout);
    }

    // Private

    /**
     * Idempotent.
     */
    private String quote(String s) {
	if (s.startsWith("\"") && s.endsWith("\"")) {
	    return s;
	} else {
	    return new StringBuffer("\"").append(s).append("\"").toString();
	}
    }
}
