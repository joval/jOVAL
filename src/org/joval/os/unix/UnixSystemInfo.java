// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Tool for creating a SystemInfoType from a Unix ISession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSystemInfo {
    private IUnixSession session;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public UnixSystemInfo(IUnixSession session) {
	this.session = session;
    }

    public SystemInfoType getSystemInfo() {
	if (info != null) {
	    return info;
	}

	//
	// Hostname
	//
	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	try {
	    info.setPrimaryHostName(SafeCLI.exec("hostname", session, IUnixSession.TIMEOUT_S));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PLUGIN_HOSTNAME);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// OS Version
	//
	try {
	    switch(session.getFlavor()) {
	      case AIX:
		info.setOsVersion(SafeCLI.exec("uname -v", session, IUnixSession.TIMEOUT_S));
		break;

	      default:
		info.setOsVersion(SafeCLI.exec("uname -r", session, IUnixSession.TIMEOUT_S));
		break;
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PLUGIN_OSVERSION);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// OS Name
	//
	try {
	    switch(session.getFlavor()) {
	      case AIX:
		info.setOsName("AIX");
		break;

	      default:
		IFilesystem fs = session.getFilesystem();
		for (INode node : fs.getFile("/etc").getChildren(Pattern.compile("^.*-release$"))) {
		    info.setOsName(SafeCLI.exec("cat " + node.getPath(), session, IUnixSession.TIMEOUT_S));
		    break;
		}
		if (!info.isSetOsName()) {
		    info.setOsName(session.getFlavor().getOsName());
		}
		break;
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PLUGIN_OSNAME);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// Processor Architecture
	//
	try {
	    info.setArchitecture(SafeCLI.exec("uname -p", session, IUnixSession.TIMEOUT_S));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PLUGIN_ARCH);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// Network Interfaces
	//
	try {
	    InterfacesType interfacesType = JOVALSystem.factories.sc.core.createInterfacesType();
	    List<NetworkInterface> interfaces = NetworkInterface.getInterfaces(session);
	    for (NetworkInterface intf : interfaces) {
		InterfaceType interfaceType = JOVALSystem.factories.sc.core.createInterfaceType();
		interfaceType.setMacAddress(intf.getMacAddress());
		interfaceType.setInterfaceName(intf.getDescription());

		if (intf.getIpV4Address() != null) {
		    interfaceType.setIpAddress(intf.getIpV4Address());
		} else if (intf.getIpV6Address() != null) {
		    interfaceType.setIpAddress(intf.getIpV6Address());
		}

		if (interfaceType.getIpAddress() != null) {
		    interfacesType.getInterface().add(interfaceType);
		}
	    }
	    info.setInterfaces(interfacesType);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PLUGIN_INTERFACE);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return info;
    }
}
