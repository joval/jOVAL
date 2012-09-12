// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.ssh.system;

import java.io.IOException;

import org.joval.intf.system.IProcess;
import org.joval.util.SessionException;

/**
 * An interface to a shell channel.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IShell extends IProcess {
    /**
     * Waiting at a prompt?
     */
    boolean ready();

    /**
     * Send a line of text.
     *
     * @throws IllegalStateException if not ready
     * @throws SessionException if there was a problem with the session
     */
    void println(String str) throws IllegalStateException, SessionException, IOException;

    /**
     * Read from the shell until there is either (1) a new prompt, or (2) the timeout has been reached.
     *
     * @throws IllegalStateException if the shell is being used to run an IProcess
     * @throws InterruptedIOException if the timeout expires
     */
    String read(long timeout) throws IllegalStateException, IOException;

    /**
     * Read a single line from the shell until there is either (1) a new prompt, or (2) the timeout has been reached.
     *
     * @throws IllegalStateException if the shell is being used to run an IProcess
     * @throws InterruptedIOException if the timeout expires
     */
    String readLine(long timeout) throws IllegalStateException, IOException;

    /**
     * Get the current prompt String.
     */
    String getPrompt();

    /**
     * Close the shell. If keepAlive=true, this call is ignored.
     */
    void close() throws IllegalStateException;
}
