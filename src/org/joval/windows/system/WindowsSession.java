// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.windows.system;

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
import org.joval.util.BaseSession;
import org.joval.util.JOVALSystem;
import org.joval.windows.WOW3264PathRedirector;
import org.joval.windows.registry.Registry;
import org.joval.windows.wmi.WmiProvider;

/**
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
		fs = new LocalFilesystem(env, new WOW3264PathRedirector(env));
	    } else {
		fs = new LocalFilesystem(env, null);
	    }
	    cwd = new File(env.expand("%SystemRoot%"));
	    return wmi.connect();
	} else {
	    return false;
	}
    }

    public void disconnect() {
	wmi.disconnect();
    }

    public int getType() {
	return WINDOWS;
    }
}
