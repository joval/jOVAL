// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.discovery;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import org.joval.intf.system.IBaseSession;

/**
 * An interface for getting a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISessionFactory {
    String LOCALHOST = "localhost";

    /**
     * Provide a directory where the SessionFactory can cache information about hosts across invocations.
     */
    void setDataDirectory(File dir) throws IOException;

    IBaseSession createSession(String hostname) throws UnknownHostException;
}
