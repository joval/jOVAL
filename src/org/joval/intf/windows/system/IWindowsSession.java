// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.windows.system;

import org.joval.intf.system.ISession;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.wmi.IWmiProvider;

/**
 * A representation of a Windows command-line session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWindowsSession extends ISession {
    void set64BitRedirect(boolean redirect64);
    IRegistry getRegistry();
    IWmiProvider getWmiProvider();
}
