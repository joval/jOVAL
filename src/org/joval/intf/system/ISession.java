// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import org.joval.intf.io.IFilesystem;

/**
 * A representation of a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISession {
    int UNIX = 1;
    int WINDOWS = 2;

    public boolean connect();

    public void disconnect();

    public void setWorkingDir(String path);

    public int getType();

    public IFilesystem getFilesystem();

    public IEnvironment getEnvironment();

    public IProcess createProcess(String command) throws Exception;
}
