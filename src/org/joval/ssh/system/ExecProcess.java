// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.File;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.exception.JSchException;

import org.joval.util.JOVALMsg;

/**
 * An SSH exec-channel-based IProcess implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class ExecProcess extends SshProcess {
    /**
     * Create an IProcess based on an SSH exec channel.
     */
    ExecProcess(Session session, String command, String[] env, boolean debug, File wsdir, int pid, LocLogger logger)
		throws JSchException {

	super(command, env, debug, wsdir, pid, logger);
	channel = (ChannelExec)session.openChannel(ChannelType.EXEC);
    }

    // Implement IProcess

    @Override
    public void start() throws Exception {
	logger.debug(JOVALMsg.STATUS_SSH_PROCESS_START, "EXEC", command);
	((ChannelExec)channel).setPty(interactive);
	((ChannelExec)channel).setCommand(command);
	channel.connect();
	running = true;
    }
}
