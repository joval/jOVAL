// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.windows.system;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.wmi.IWmiProvider;

/**
 * A representation of a Windows session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWindowsSession extends ISession {
    public enum View {
	_32BIT,
	_64BIT;
    }

    String ENV_ARCH = "PROCESSOR_ARCHITECTURE";

    IRegistry getRegistry(View view);

    boolean supports(View view);

    /**
     * As an ISession, the getFilesystem() call always returns a non-redirected view.
     */
    IFilesystem getFilesystem(View view);

    IWmiProvider getWmiProvider();

    IDirectory getDirectory();
}
