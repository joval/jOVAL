// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.powershell;

import java.io.InputStream;
import java.io.IOException;

/**
 * An interface to a powershell runspace.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRunspace {
    /**
     * Get a unique identifier for this runspace.
     */
    String getId();

    /**
     * Load a Powershell module into the runspace from a stream.
     */
    void loadModule(InputStream in, long timeout) throws IOException;

    /**
     * Invoke a command or module.
     */
    void invoke(String command) throws IOException;

    /**
     * Read until there is either (1) a new prompt, or (2) the timeout has been reached.
     *
     * @throws InterruptedIOException if the timeout expires
     */
    String read(long timeout) throws IOException;

    /**
     * Read until there is either (1) a line break, (2) a new prompt, or (3) the timeout has been reached.
     *
     * @throws InterruptedIOException if the timeout expires
     */
    String readLine(long timeout) throws IOException;

    /**
     * Get the current prompt String.
     */
    String getPrompt();

    /**
     * Determine whether an error was detected during the last read.
     */
    boolean hasError();

    /**
     * Retrieve any error messages generated since the last time this method was invoked.
     */
    String getError();
}
