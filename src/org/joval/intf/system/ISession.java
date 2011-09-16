// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.IPathRedirector;

/**
 * A representation of a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISession extends IBaseSession {
    public void setWorkingDir(String path);

    public IFilesystem getFilesystem();

    public IEnvironment getEnvironment();
}
