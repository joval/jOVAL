// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.ssh.system;

import org.joval.intf.system.IBaseSession;

public interface ISshSession extends IBaseSession {
    /**
     * Property indicating whether to log messages from JSch to the JOVALSystem logger (true/false).
     */
    String PROP_DEBUG = "ssh.debug";

    /**
     * Property indicating whether to log messages from JSch to the JOVALSystem logger (true/false).
     */
    String PROP_ATTACH_LOG = "ssh.attach.log";

    /**
     * Property indicating the number of milliseconds to wait before failing to establish an SSH connection.
     */
    String PROP_CONNECTION_TIMEOUT = "ssh.conn.timeout";

    /**
     * Property indicating the number of times to re-try establishing an SSH connection in the event of a failure.
     */
    String PROP_CONNECTION_RETRIES = "ssh.conn.retries";

    /**
     * Property indicating the number of times to re-try running a command on a non-Unix SSH-enabled device in the event of an
     * unexpected disconnect.
     */
    String PROP_EXEC_RETRIES = "exec.retries.ssh";
}
