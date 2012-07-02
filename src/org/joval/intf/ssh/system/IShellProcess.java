// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.ssh.system;

import org.joval.intf.system.IProcess;

/**
 * An interface to a process implemented on top of a shell.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IShellProcess extends IProcess {
    /**
     * Set whether the shell channel should be closed when the process ends, or kept alive for re-use.
     */
    void setKeepAlive(boolean keepAlive);

    /**
     * Is the SSH channel connected?  (False until first start or getShell().println);
     */
    boolean isAlive();

    /**
     * Execute another process in this (previously opened) shell.
     */
    IProcess newProcess(String command) throws IllegalStateException;

    /**
     * Get access to the shell.
     */
    IShell getShell() throws IllegalStateException;
}
