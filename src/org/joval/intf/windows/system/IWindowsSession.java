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
    /**
     * Property governing the method use to control the execution of processes on remote Windows machines.
     */
    String PROP_REMOTE_EXEC_IMPL = "remote.exec.method";

    /**
     * Property governing whether or not to encrypt WS-Management SOAP envelopes.
     */
    String PROP_WSMAN_ENCRYPT = "remote.wsman.encryption";

    /**
     * Specifies a remote execution implementation based on WMI-over-DCOM.
     */
    String VAL_WMI = "wmi";

    /**
     * Specifies a remote execution implementation based on Windows Remote Management Web Services (MS-WSMV).
     */
    String VAL_WINRM = "winrm";

    String ENV_ARCH = "PROCESSOR_ARCHITECTURE";

    public enum View {
	_32BIT,
	_64BIT;
    }

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
