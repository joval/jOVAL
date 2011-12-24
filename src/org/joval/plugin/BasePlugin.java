// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.util.Collection;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.OvalException;
import org.joval.plugin.adapter.cisco.ios.LineAdapter;
import org.joval.plugin.adapter.cisco.ios.VersionAdapter;
import org.joval.plugin.adapter.cisco.ios.Version55Adapter;
import org.joval.plugin.adapter.independent.Environmentvariable58Adapter;
import org.joval.plugin.adapter.independent.EnvironmentvariableAdapter;
import org.joval.plugin.adapter.independent.FamilyAdapter;
import org.joval.plugin.adapter.independent.TextfilecontentAdapter;
import org.joval.plugin.adapter.independent.Textfilecontent54Adapter;
import org.joval.plugin.adapter.independent.VariableAdapter;
import org.joval.plugin.adapter.independent.XmlfilecontentAdapter;
import org.joval.plugin.adapter.linux.RpminfoAdapter;
import org.joval.plugin.adapter.macos.PlistAdapter;
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
 * The abstract base class for all functional jovaldi plug-ins.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BasePlugin implements IPlugin {
    private String hostname;

    protected LocLogger logger;
    protected ISession session;
    protected Collection<IAdapter> adapters;

    protected BasePlugin() {
	logger = JOVALSystem.getLogger();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IPlugin

    public Collection<IAdapter> getAdapters() {
	if (adapters == null) {
	    if (session == null) {
		return null;
	    }

	    adapters = new Vector<IAdapter>();
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

	    JOVALSystem.getLogger().trace(JOVALMsg.STATUS_SESSION_TYPE, session.getType());
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
		IUnixSession.Flavor flavor = unix.getFlavor();
		switch(flavor) {
		  case LINUX:
		    adapters.add(new RpminfoAdapter(unix));
		    break;

		  case MACOSX:
		    adapters.add(new PlistAdapter(unix));
		    break;

		  case SOLARIS:
		    adapters.add(new IsainfoAdapter(unix));
		    adapters.add(new PackageAdapter(unix));
		    adapters.add(new Patch54Adapter(unix));
		    adapters.add(new PatchAdapter(unix));
		    adapters.add(new SmfAdapter(unix));
		    break;

		  default:
		    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, flavor);
		    break;
		}
		break;
	      }

	      case CISCO_IOS: {
		adapters.add(new LineAdapter(session));
		adapters.add(new VersionAdapter(session));
		adapters.add(new Version55Adapter(session));
		break;
	      }
	    }
	}
	return adapters;
    }

    public void connect() throws OvalException {
	logger.info(JOVALMsg.STATUS_PLUGIN_CONNECT);
	if (session == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_NONE));
	} else {
	    session.setLogger(logger);
	    if (!session.connect()) {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
	    }
	}
    }

    public void disconnect() {
	logger.info(JOVALMsg.STATUS_PLUGIN_DISCONNECT);
	if (session != null) {
	    session.disconnect();
	}
    }

    public SystemInfoType getSystemInfo() {
	return session.getSystemInfo();
    }
}
