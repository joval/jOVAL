// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.system;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.io.LocalFilesystem;
import org.joval.os.windows.io.WOW3264FilesystemRedirector;
import org.joval.os.windows.registry.Registry;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
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
    private boolean is64bit = false;
    private Registry reg32, reg;
    private IFilesystem fs32;

    public WindowsSession() {
	super();
    }

    // Implement IWindowsSession extensions

    public IRegistry getRegistry(View view) {
	switch(view) {
	  case _32BIT:
	    return reg32;
	}
	return reg;
    }

    public boolean supports(View view) {
	switch(view) {
	  case _32BIT:
	    return true;
	  case _64BIT:
	  default:
	    return is64bit;
	}
    }

    public IFilesystem getFilesystem(View view) {
	switch(view) {
	  case _32BIT:
	    return fs32;
	}
	return fs;
    }

    public IWmiProvider getWmiProvider() {
	return wmi;
    }

    // Implement ISession

    public boolean connect() {
	reg = new Registry(null);
	wmi = new WmiProvider();
	if (reg.connect()) {
	    env = reg.getEnvironment();
	    fs = new LocalFilesystem(env, null);
	    is64bit = env.getenv(ENV_ARCH).indexOf("64") != -1;
	    if (is64bit) {
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_WINDOWS_BITNESS", "64"));
		WOW3264RegistryRedirector.Flavor flavor = WOW3264RegistryRedirector.getFlavor(reg);
		reg32 = new Registry(new WOW3264RegistryRedirector(flavor));
		fs32 = new LocalFilesystem(env, new WOW3264FilesystemRedirector(env));
	    } else {
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_WINDOWS_BITNESS", "32"));
		reg32 = reg;
		fs32 = fs;
	    }
	    reg.disconnect();
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
