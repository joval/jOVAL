// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A representation of a process.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProcess {
    public void setInteractive(boolean interactive);

    public void start() throws Exception;

    public InputStream getInputStream();

    public InputStream getErrorStream();

    public OutputStream getOutputStream();

    public void waitFor(long millis) throws InterruptedException;

    public int exitValue() throws IllegalThreadStateException;

    public void destroy();
}
