// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.di.IJovaldiPlugin;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.embedded.IosSystemInfo;
import org.joval.os.unix.UnixSystemInfo;
import org.joval.os.windows.WindowsSystemInfo;
import org.joval.plugin.adapter.cisco.ios.LineAdapter;
import org.joval.plugin.adapter.cisco.ios.Version55Adapter;
import org.joval.plugin.adapter.independent.Environmentvariable58Adapter;
import org.joval.plugin.adapter.independent.EnvironmentvariableAdapter;
import org.joval.plugin.adapter.independent.FamilyAdapter;
import org.joval.plugin.adapter.independent.TextfilecontentAdapter;
import org.joval.plugin.adapter.independent.Textfilecontent54Adapter;
import org.joval.plugin.adapter.independent.VariableAdapter;
import org.joval.plugin.adapter.independent.XmlfilecontentAdapter;
import org.joval.plugin.adapter.linux.RpminfoAdapter;
import org.joval.plugin.adapter.solaris.IsainfoAdapter;
import org.joval.plugin.adapter.solaris.PackageAdapter;
import org.joval.plugin.adapter.solaris.Patch54Adapter;
import org.joval.plugin.adapter.solaris.PatchAdapter;
import org.joval.plugin.adapter.solaris.SmfAdapter;
import org.joval.plugin.adapter.unix.ProcessAdapter;
import org.joval.plugin.adapter.unix.RunlevelAdapter;
import org.joval.plugin.adapter.unix.UnameAdapter;
import org.joval.plugin.adapter.windows.Fileeffectiverights53Adapter;
import org.joval.plugin.adapter.windows.GroupAdapter;
import org.joval.plugin.adapter.windows.GroupSidAdapter;
import org.joval.plugin.adapter.windows.RegistryAdapter;
import org.joval.plugin.adapter.windows.SidAdapter;
import org.joval.plugin.adapter.windows.SidSidAdapter;
import org.joval.plugin.adapter.windows.UserAdapter;
import org.joval.plugin.adapter.windows.UserSid55Adapter;
import org.joval.plugin.adapter.windows.UserSidAdapter;
import org.joval.plugin.adapter.windows.Wmi57Adapter;
import org.joval.plugin.adapter.windows.WmiAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Abstract base class for jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class OnlinePlugin extends OfflinePlugin {
    protected ISession session;
    protected String err;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    protected OnlinePlugin() {
	super();
    }

    // Implement IJovaldiPlugin

    public void connect() {
	if (session.connect()) {
	    adapters.add(new FamilyAdapter(session));
	    adapters.add(new VariableAdapter());
	    if (session.getEnvironment() != null) {
		adapters.add(new Environmentvariable58Adapter(session));
		adapters.add(new EnvironmentvariableAdapter(session));
	    }
	    if (session.getFilesystem() != null) {
		adapters.add(new Textfilecontent54Adapter(session));
		adapters.add(new TextfilecontentAdapter(session));
		adapters.add(new XmlfilecontentAdapter(session));
	    }

	    JOVALSystem.getLogger().trace(JOVALMsg.STATUS_SESSIONTYPE, session.getType());
	    switch(session.getType()) {
	      case WINDOWS: {
		IWindowsSession win = (IWindowsSession)session;
		adapters.add(new org.joval.plugin.adapter.windows.FileAdapter(win));
		adapters.add(new Fileeffectiverights53Adapter(win));
		adapters.add(new RegistryAdapter(win));
		adapters.add(new Wmi57Adapter(win));
		adapters.add(new WmiAdapter(win));
		adapters.add(new GroupAdapter(win));
		adapters.add(new GroupSidAdapter(win));
		adapters.add(new SidAdapter(win));
		adapters.add(new SidSidAdapter(win));
		adapters.add(new UserAdapter(win));
		adapters.add(new UserSid55Adapter(win));
		adapters.add(new UserSidAdapter(win));
		break;
	      }

	      case UNIX: {
		IUnixSession unix = (IUnixSession)session;
		adapters.add(new org.joval.plugin.adapter.unix.FileAdapter(unix));
		adapters.add(new ProcessAdapter(unix));
		adapters.add(new RunlevelAdapter(unix));
		adapters.add(new UnameAdapter(unix));
		switch(unix.getFlavor()) {
		  case LINUX:
		    adapters.add(new RpminfoAdapter(unix));
		    break;
		  case SOLARIS:
		    adapters.add(new IsainfoAdapter(unix));
		    adapters.add(new PackageAdapter(unix));
		    adapters.add(new Patch54Adapter(unix));
		    adapters.add(new PatchAdapter(unix));
		    adapters.add(new SmfAdapter(unix));
		    break;
		}
		break;
	      }

	      case CISCO_IOS: {
		adapters.add(new LineAdapter(session));
		adapters.add(new Version55Adapter(session));
		break;
	      }
	    }
	} else {
	    throw new RuntimeException(getMessage("ERROR_SESSION_CONNECTION"));
	}
    }

    public void disconnect() {
	if (session != null) {
	    session.disconnect();
	}
    }

    public abstract boolean configure(Properties props);

    public String getLastError() {
	return err;
    }

    /**
     * @override
     */
    public SystemInfoType getSystemInfo() {
	return session.getSystemInfo();
    }
}
