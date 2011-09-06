// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.system;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.io.LocalFilesystem;
import org.joval.os.windows.WOW3264PathRedirector;
import org.joval.os.windows.registry.Registry;
import org.joval.os.windows.wmi.WmiProvider;
import org.joval.util.BaseSession;
import org.joval.util.JOVALSystem;

/**
 * Windows implementation of ISession for local machines, using JNA and JACOB.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSession extends BaseSession implements IWindowsSession {
    private WmiProvider wmi;
    private Registry registry;
    private boolean redirect64 = true;

    public WindowsSession(boolean redirect64) {
	super();
    }

    // Implement IWindowsSession extensions

    public void set64BitRedirect(boolean redirect64) {
	this.redirect64 = redirect64;
    }

    public IRegistry getRegistry() {
	return registry;
    }

    public IWmiProvider getWmiProvider() {
	return wmi;
    }

    // Implement ISession

    public boolean connect() {
	registry = new Registry();
	registry.set64BitRedirect(redirect64);
	wmi = new WmiProvider();
	if (registry.connect()) {
	    env = registry.getEnvironment();
	    registry.disconnect();
	    if (redirect64 && registry.is64Bit()) {
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_WINDOWS_REDIRECT", "true"));
		fs = new LocalFilesystem(env, new WOW3264PathRedirector(env));
	    } else {
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_WINDOWS_REDIRECT", "false"));
		fs = new LocalFilesystem(env, null);
	    }
	    cwd = new File(env.expand("%SystemRoot%"));
	    return wmi.connect();
	} else {
	    return false;
	}
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	}
    }

    public Type getType() {
	return Type.WINDOWS;
    }
}
