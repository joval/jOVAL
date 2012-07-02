// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.OutputStream;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelShell;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.io.PerishableReader;
import org.joval.intf.io.IReader;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An SSH shell-channel-based IProcess implementation for POSIX-compliant systems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class PosixShellProcess extends BasicShellProcess {
    private String[] env;

    /**
     * Create a shell-based IProcess for a POSIX system.  Permits the setting of arbitrary environment variables.
     */
    PosixShellProcess(Session session, String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger)
		throws JSchException {

	super(session, command, debug, wsdir, pid, logger);
	this.env = env;
	type = Type.POSIX;
    }

    // Implement IProcess

    @Override
    public void start() throws Exception {
	switch(mode) {
	  case SHELL:
	  case INACTIVE:
	    throw new IllegalStateException(mode.toString());
	}

	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_START, type, command);
	if (!channel.isConnected()) {
	    connect();
	}
	out.write("/bin/sh\r".getBytes(StringTools.ASCII));
	out.flush();
	readInternal(10000L);
	PerishableReader reader = PerishableReader.newInstance(in, 10000L);
	if (env != null) {
	    for (String var : env) {
		int ptr = var.indexOf("=");
		if (ptr > 0) {
		    StringBuffer setenv = new StringBuffer(var).append("; export ").append(var.substring(0,ptr));
		    out.write(setenv.toString().getBytes(StringTools.ASCII));
		    out.write(CR);
		    out.flush();
		    reader.readUntil(prompt); // ignore
		}
	    }
	}
	in = new MarkerTerminatedInputStream(reader, prompt.getBytes(StringTools.ASCII));
	out.write(command.getBytes(StringTools.ASCII));
	out.write(CR);
	out.flush();
	running = true;
    }

    // Internal

    @Override
    int getExitValueInternal(IReader reader) throws Exception {
	out.write("echo $?".getBytes(StringTools.ASCII));
	out.write(CR);
	out.flush();
	int ec = Integer.parseInt(reader.readLine());
	while (reader.readLine() != null) {} // advance to the end of the reader
	return ec;
    }
}
