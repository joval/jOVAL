// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

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
	try {
	    info.setPrimaryHostName(SafeCLI.exec("hostname", session, IUnixSession.Timeout.S));
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_SYSINFO_HOSTNAME);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

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
		for (IFile file : fs.getFile("/etc", IFile.NOCACHE).listFiles(Pattern.compile("^.*-release$"))) {
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
