// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.io.IReader;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.systemcharacteristics.core.InterfacesType;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.SystemInfoType;

import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Tool for creating a SystemInfoType from a Unix ISession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class UnixSystemInfo {
    static SystemInfoType getSystemInfo(IUnixSession session) {
	//
	// Hostname
	//
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setPrimaryHostName(session.getMachineName());

	//
	// OS Version
	//
	try {
	    switch(session.getFlavor()) {
	      case AIX:
		info.setOsVersion(SafeCLI.exec("oslevel -r", session, IUnixSession.Timeout.S));
		break;

	      default:
		info.setOsVersion(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
		break;
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSVERSION);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
		for (IFile file : fs.getFile("/etc", IFile.Flags.NOCACHE).listFiles(Pattern.compile("^.*-release$"))) {
		    info.setOsName(SafeCLI.exec("cat " + file.getPath(), session, IUnixSession.Timeout.S));
		    break;
		}
		if (!info.isSetOsName()) {
		    info.setOsName(session.getFlavor().value());
		}
		break;
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_OSNAME);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// Processor Architecture
	//
	try {
	    info.setArchitecture(SafeCLI.exec("uname -p", session, IUnixSession.Timeout.S));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_ARCH);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	//
	// Network Interfaces
	//
	try {
	    InterfacesType interfacesType = Factories.sc.core.createInterfacesType();
	    List<UnixNetworkInterface> interfaces = UnixNetworkInterface.getInterfaces(session);
	    for (UnixNetworkInterface intf : interfaces) {
		InterfaceType interfaceType = Factories.sc.core.createInterfaceType();
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
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_INTERFACE);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return info;
    }
}
