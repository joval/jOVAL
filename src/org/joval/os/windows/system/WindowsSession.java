// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.system;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.io.LocalFilesystem;
import org.joval.os.windows.WindowsSystemInfo;
import org.joval.os.windows.identity.Directory;
import org.joval.os.windows.io.WOW3264FilesystemRedirector;
import org.joval.os.windows.registry.Registry;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
import org.joval.os.windows.wmi.WmiProvider;
import org.joval.util.AbstractSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

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
    private IFilesystem fs32;
    private WindowsSystemInfo info = null;
    private Directory directory = null;

    public WindowsSession() {
	super();
	info = new WindowsSystemInfo(this);
    }

    // Implement IWindowsSession extensions

    public IDirectory getDirectory() {
	if (directory == null) {
	    directory = new Directory(this);
	}
	return directory;
    }

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
	if (wmi == null) {
	    wmi = new WmiProvider(this);
	}
	return wmi;
    }

    // Implement ILoggable

    /**
     * @override
     */
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (fs32 != null && !fs32.equals(fs)) {
	    fs32.setLogger(logger);
	}
    }

    // Implement IBaseSession

    public String getHostname() {
	return LOCALHOST;
    }

    public boolean connect() {
	reg = new Registry(null, this);
	wmi = new WmiProvider(this);
	if (reg.connect()) {
	    env = reg.getEnvironment();
	    fs = new LocalFilesystem(this, env, null);
	    is64bit = env.getenv(ENV_ARCH).indexOf("64") != -1;
	    if (is64bit) {
		if (!"64".equals(System.getProperty("sun.arch.data.model"))) {
		    reg.disconnect();
		    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINDOWS_BITNESS_INCOMPATIBLE));
		}
		logger.trace(JOVALMsg.STATUS_WINDOWS_BITNESS, "64");
		WOW3264RegistryRedirector.Flavor flavor = WOW3264RegistryRedirector.getFlavor(reg);
		reg32 = new Registry(new WOW3264RegistryRedirector(flavor), this);
		fs32 = new LocalFilesystem(this, env, new WOW3264FilesystemRedirector(env));
	    } else {
		logger.trace(JOVALMsg.STATUS_WINDOWS_BITNESS, "32");
		reg32 = reg;
		fs32 = fs;
	    }
	    cwd = new File(env.expand("%SystemRoot%"));
	    try {
		if (wmi.connect()) {
		    directory = new Directory(this);
		    directory.connect();
		    info.getSystemInfo();
		    return true;
		} else {
		    logger.warn(JOVALMsg.ERROR_WINWMI_CONNECT);
		    return false;
		}
	    } finally {
		reg.disconnect();
	    }
	} else {
	    logger.warn(JOVALMsg.ERROR_WINREG_CONNECT);
	    return false;
	}
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	    wmi = null;
	}
	if (directory != null) {
	    directory.disconnect();
	    directory = null;
	}
    }

    public Type getType() {
	return Type.WINDOWS;
    }

    // Implement ISession

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }
}
