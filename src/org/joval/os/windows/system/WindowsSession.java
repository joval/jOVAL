// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.system;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.io.AbstractFilesystem;
import org.joval.os.windows.identity.Directory;
import org.joval.os.windows.io.WindowsFilesystem;
import org.joval.os.windows.powershell.RunspacePool;
import org.joval.os.windows.registry.Registry;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
import org.joval.os.windows.wmi.WmiProvider;
import org.joval.util.AbstractSession;
import org.joval.util.JOVALMsg;

/**
 * Windows implementation of ISession for local machines, using JNA and JACOB.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSession extends AbstractSession implements IWindowsSession {
    private WmiProvider wmi;
    private boolean is64bit = false;
    private Registry reg32, reg;
    private IWindowsFilesystem fs32;
    private Directory directory = null;
    private RunspacePool runspaces = null;

    //
    // Load the JACOB DLL
    //
    static {
	if ("32".equals(System.getProperty("sun.arch.data.model"))) {
	    System.loadLibrary("jacob-1.15-M4-x86");
	} else {
	    System.loadLibrary("jacob-1.15-M4-x64");
	}
    }

    public WindowsSession(File wsdir) {
	super();
	this.wsdir = wsdir;
    }

    // Implement IWindowsSession extensions

    public IRunspacePool getRunspacePool() {
	return runspaces;
    }

    public IDirectory getDirectory() {
	return directory;
    }

    public String getMachineName() {
	if (isConnected()) {
	    try {
		IKey key = reg.fetchKey(IRegistry.HKLM, IRegistry.COMPUTERNAME_KEY);
		IValue val = key.getValue(IRegistry.COMPUTERNAME_VAL);
		if (val.getType() == IValue.REG_SZ) {
		    return ((IStringValue)val).getData();
		} else {
		    logger.warn(JOVALMsg.ERROR_SYSINFO_HOSTNAME);
		}
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_SYSINFO_HOSTNAME);
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return getHostname();
    }

    public View getNativeView() {
	return is64bit ? View._64BIT : View._32BIT;
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

    public IRegistry getRegistry(View view) {
	switch(view) {
	  case _32BIT:
	    if (reg32 == null) {
		if (getNativeView() == View._32BIT) {
		    reg32 = reg;
		} else {
		    reg32 = new Registry(this, View._32BIT);
		}
	    }
	    return reg32;

	  default:
	    return reg;
	}
    }

    public IWindowsFilesystem getFilesystem(View view) {
	switch(view) {
	  case _32BIT:
	    if (fs32 == null) {
		if (getNativeView() == View._32BIT) {
		    fs32 = (IWindowsFilesystem)fs;
		} else {
		    try {
			fs32 = new WindowsFilesystem(this, View._32BIT);
		    } catch (Exception e) {
			logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	    return fs32;

	  default:
	    return (IWindowsFilesystem)fs;
	}
    }

    public IWmiProvider getWmiProvider() {
	return wmi;
    }

    // Implement ILoggable

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (fs32 != null && !fs32.equals(fs)) {
	    fs32.setLogger(logger);
	}
	if (wmi != null) {
	    wmi.setLogger(logger);
	}
	if (directory != null) {
	    directory.setLogger(logger);
	}
    }

    // Implement IBaseSession

    @Override
    public void dispose() {
	super.dispose();
	if (fs32 instanceof AbstractFilesystem) {
	    ((AbstractFilesystem)fs32).dispose();
	}
    }

    public boolean connect() {
	if (env == null) {
	    try {
		env = new Environment(this);
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		return false;
	    }
	}
	is64bit = ((Environment)env).is64bit();
	if (is64bit) {
	    if (!"64".equals(System.getProperty("sun.arch.data.model"))) {
		throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINDOWS_BITNESS_INCOMPATIBLE));
	    }
	    logger.trace(JOVALMsg.STATUS_WINDOWS_BITNESS, "64");
	} else {
	    logger.trace(JOVALMsg.STATUS_WINDOWS_BITNESS, "32");
	}

	if (runspaces == null) {
	    runspaces = new RunspacePool(this, 100);
	}
	if (reg == null) {
	    reg = new Registry(this);
	    if (!is64bit) reg32 = reg;
	}
	if (fs == null) {
	    try {
		fs = new WindowsFilesystem(this);
		if (!is64bit) fs32 = (IWindowsFilesystem)fs;
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		return false;
	    }
	}
	cwd = new File(env.expand("%SystemRoot%"));
	if (wmi == null) {
	    wmi = new WmiProvider(this);
	}
	if (wmi.register()) {
	    connected = true;
	    if (directory == null) {
		directory = new Directory(this);
	    }
	    directory.setWmiProvider(wmi);
	    return true;
	} else {
	    return false;
	}
    }

    public void disconnect() {
	runspaces.shutdown();
	wmi.deregister();
	connected = false;
    }

    public Type getType() {
	return Type.WINDOWS;
    }
}
