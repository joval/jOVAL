// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.system;

import org.joval.intf.system.ISession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.wmi.IWmiProvider;

/**
 * A representation of a Windows session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWindowsSession extends ISession {
    String ENV_ARCH = "PROCESSOR_ARCHITECTURE";
    String ENV_AR32 = "PROCESSOR_ARCHITEW6432";

    public enum View {
	_32BIT,
	_64BIT;
    }

    View getNativeView();

    /**
     * Get the machine name. NOTE: This name might not be meaningful to DNS.
     */
    String getMachineName();

    IRegistry getRegistry(View view);

    boolean supports(View view);

    /**
     * As an ISession, the getFilesystem() call always returns a non-redirected view.
     */
    IWindowsFilesystem getFilesystem(View view);

    IWmiProvider getWmiProvider();

    IDirectory getDirectory();

    IRunspacePool getRunspacePool();
}
