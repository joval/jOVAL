// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.powershell;

import java.io.InputStream;
import java.io.IOException;

import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;

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
     *
     * @throws IOException if there is a problem reading from the input, or writing to the Runspace
     * @throws PowershellException if there is a Powershell syntactical error with the module contents
     */
    void loadModule(InputStream in) throws IOException, PowershellException;

    /**
     * Load a module with the specified read timeout (in millis).
     */
    void loadModule(InputStream in, long timeout) throws IOException, PowershellException;

    /**
     * Invoke a command or module.
     *
     * @returns Text output from the command
     *
     * @throws IOException if there is a problem reading or writing data to/from the Runspace
     * @throws PowershellException if the command causes Powershell to raise an exception
     */
    String invoke(String command) throws IOException, PowershellException;

    /**
     * Invoke a command or module with the specified read timeout (in millis).
     */
    String invoke(String command, long timeout) throws IOException, PowershellException;

    /**
     * Get the current prompt String.
     */
    String getPrompt();

    /**
     * Get the view for this runspace.
     */
    IWindowsSession.View getView();
}
