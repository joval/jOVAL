// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface representation of a process.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProcess {
    /**
     * Set the interactive property for the process.
     */
    public void setInteractive(boolean interactive);

    /**
     * Start the process.
     */
    public void start() throws Exception;

    /**
     * Get the process's stdout
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Get the process's stderr
     */
    public InputStream getErrorStream() throws IOException;

    /**
     * Get the process's stdin
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     * Wait for the process to complete.
     *
     * @param millis The maximum number of milliseconds to wait.  Set to 0 to wait until the process finishes (potentially
     *               forever.
     */
    public void waitFor(long millis) throws InterruptedException;

    /**
     * Get the exit code returned by the process.
     *
     * @throws IllegalThreadStateException if the process is still running, or was not started.
     */
    public int exitValue() throws IllegalThreadStateException;

    /**
     * Destroy the process. Generally this is called if the process has stopped responding, or if it's taking too long to
     * return.
     */
    public void destroy();
}
