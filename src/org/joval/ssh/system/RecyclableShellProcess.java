// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import org.vngx.jsch.Session;

import org.joval.intf.system.IProcess;

/**
 * An interface defining methods for a Shell-derived process handle that can be re-used, i.e., it is not
 * closed between command invocations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface RecyclableShellProcess extends IProcess {
    /**
     * Enable/Disable the reusability feature.
     */
    void setKeepAlive(boolean keepAlive);

    /**
     * Repurpose the shell channel for a new command.  The last command must have already completed.
     * Calling start() after this method will run the command specified here.
     */
    void recycle(String command) throws IllegalStateException;
}
