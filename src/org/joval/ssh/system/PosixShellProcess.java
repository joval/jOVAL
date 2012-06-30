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
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An SSH shell-channel-based IProcess implementation for POSIX-compliant systems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class PosixShellProcess extends BasicShellProcess {
    /**
     * Create a shell-based IProcess for a POSIX system.  Permits the setting of arbitrary environment variables.
     */
    PosixShellProcess(Session session, String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger)
		throws JSchException {

	super(session, command, env, debug, wsdir, pid, logger);
	keepAlive = false; // always close the channel after determining the exit code
    }

    // Implement IProcess

    @Override
    public void start() throws Exception {
	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_START, Type.POSIX, command);
	((ChannelShell)channel).setPty(true);
	((ChannelShell)channel).setTerminalMode(MODE);
	channel.connect();
	getOutputStream();
	PerishableReader reader = PerishableReader.newInstance(getInputStream(), 10000L);
	determinePrompt(reader); // garbage - may include MOTD
	out.write("/bin/sh\r".getBytes(StringTools.ASCII));
	out.flush();
	String prompt = trimLeft(determinePrompt(reader));
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

    @Override
    public int exitValue() throws IllegalThreadStateException {
	if (isRunning()) {
	    throw new IllegalThreadStateException(command);
	}
	return exitValue;
    }

    // Internal

    @Override
    int getExitValueInternal(InputStream in, byte[] prompt) throws Exception {
	out.write("echo $?".getBytes(StringTools.ASCII));
	out.write(CR);
	out.flush();
	PerishableReader reader = PerishableReader.newInstance(in, 0);
	int ec = Integer.parseInt(reader.readLine());
	reader.readFully(new byte[prompt.length]);
	return ec;
    }
}
