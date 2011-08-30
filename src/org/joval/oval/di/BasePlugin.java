// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

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
import java.util.logging.Level;

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
import org.joval.os.windows.identity.ActiveDirectory;
import org.joval.os.windows.identity.LocalDirectory;
import org.joval.plugin.adapter.cisco.ios.Version55Adapter;
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
import org.joval.plugin.adapter.windows.GroupAdapter;
import org.joval.plugin.adapter.windows.GroupSidAdapter;
import org.joval.plugin.adapter.windows.RegistryAdapter;
import org.joval.plugin.adapter.windows.UserAdapter;
import org.joval.plugin.adapter.windows.UserSid55Adapter;
import org.joval.plugin.adapter.windows.UserSidAdapter;
import org.joval.plugin.adapter.windows.Wmi57Adapter;
import org.joval.plugin.adapter.windows.WmiAdapter;
import org.joval.util.JOVALSystem;

/**
 * Abstract base class for jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BasePlugin implements IJovaldiPlugin {
    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = BasePlugin.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("plugin.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("plugin.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("plugin.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	}
    }

    /**
     * Retrieve a message using its key.
     */
    protected static final String getMessage(String key, Object... arguments) {
        return MessageFormat.format(resources.getString(key), arguments);
    }

    protected ISession session;
    protected SystemInfoType info;
    protected String err;
    protected List<IAdapter> adapters;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    protected BasePlugin() {
	adapters = new Vector<IAdapter>();
    }

    // Implement IJovaldiPlugin

    public void connect(boolean online) {
	if (online && session.connect()) {
	    adapters.add(new EnvironmentvariableAdapter(session.getEnvironment()));
	    adapters.add(new FamilyAdapter(this));
	    adapters.add(new Textfilecontent54Adapter(session.getFilesystem()));
	    adapters.add(new TextfilecontentAdapter(session.getFilesystem()));
	    adapters.add(new VariableAdapter());
	    adapters.add(new XmlfilecontentAdapter(session.getFilesystem()));

	    switch(session.getType()) {
	      case WINDOWS: {
		IWindowsSession win = (IWindowsSession)session;
		//
		// Gather SystemInfo data
		//
		WindowsSystemInfo wsi = new WindowsSystemInfo(win.getRegistry(), win.getWmiProvider());
		try {
		    info = wsi.getSystemInfo();
		} catch (Exception e) {
		    throw new RuntimeException(getMessage("ERROR_INFO"), e);
		}
		adapters.add(new org.joval.plugin.adapter.windows.FileAdapter(win.getFilesystem(), win.getWmiProvider()));
		adapters.add(new RegistryAdapter(win.getRegistry()));
		adapters.add(new Wmi57Adapter(win.getWmiProvider()));
		adapters.add(new WmiAdapter(win.getWmiProvider()));

		LocalDirectory local = new LocalDirectory(info.getPrimaryHostName(), win.getWmiProvider());
		ActiveDirectory ad = new ActiveDirectory(win.getWmiProvider());
		adapters.add(new GroupAdapter(local, ad, win.getWmiProvider()));
		adapters.add(new GroupSidAdapter(local, ad, win.getWmiProvider()));
		adapters.add(new UserAdapter(local, ad, win.getWmiProvider()));
		adapters.add(new UserSid55Adapter(local, ad, win.getWmiProvider()));
		adapters.add(new UserSidAdapter(local, ad, win.getWmiProvider()));
		break;
	      }

	      case UNIX: {
		IUnixSession unix = (IUnixSession)session;
		info = new UnixSystemInfo(unix).getSystemInfo();
		adapters.add(new org.joval.plugin.adapter.unix.FileAdapter(unix, unix.getFilesystem()));
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
		info = new IosSystemInfo(session).getSystemInfo();
		adapters.add(new Version55Adapter(info.getOsVersion()));
//		adapters.add(new LineAdapter(session));
		break;
	      }
	    }
	} else if (!online) {
	    JOVALSystem.getLogger().log(Level.INFO, JOVALSystem.getMessage("STATUS_OFFLINE"));
	} else {
	    throw new RuntimeException(getMessage("ERROR_SESSION_CONNECTION"));
	}
    }

    public void disconnect() {
	if (session != null) {
	    session.disconnect();
	}
    }

    public void setDataDirectory(File dir) {
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public String getLastError() {
	return err;
    }

    public List<IAdapter> getAdapters() {
	return adapters;
    }

    public SystemInfoType getSystemInfo() {
	return info;
    }

    public FamilyEnumeration getFamily() {
	switch(session.getType()) {
	  case WINDOWS:
	    return FamilyEnumeration.WINDOWS;

	  case UNIX:
	    return FamilyEnumeration.UNIX;

	  case CISCO_IOS:
	    return FamilyEnumeration.IOS;

	  default:
	    return FamilyEnumeration.UNDEFINED;
	}
    }
}
