// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.sysinfo;

import java.util.List;
import java.util.NoSuchElementException;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.oval.Factories;

/**
 * Tool for creating a SystemInfoType from a Cisco IOS-attached ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class IosSystemInfo {
    static SystemInfoType getSystemInfo(IIosSession session) throws NoSuchElementException {
	ITechSupport techSupport = session.getTechSupport();
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setOsName("Cisco IOS");
	info.setOsVersion("unknown");
	info.setArchitecture("unknown");
	for (String line : techSupport.getLines("show running-config")) {
	    if (line.startsWith("hostname")) {
		info.setPrimaryHostName(line.substring(9).trim());
		break;
	    }
	}

	for (String line : techSupport.getLines("show version")) {
	    if (line.startsWith("Cisco IOS")) {
		int ptr = line.indexOf("Version ");
		if (ptr != -1) {
		    int begin = ptr + 8;
		    int end = line.indexOf(",", begin+1);
		    if (end == -1) {
			info.setOsVersion(line.substring(begin));
		    } else {
			info.setOsVersion(line.substring(begin, end));
		    }
		}
	    } else if (line.indexOf("CPU") != -1) {
		int ptr = line.indexOf("CPU");
		info.setArchitecture(line.substring(0, ptr).trim());
	    }
	}

	InterfacesType interfacesType = Factories.sc.core.createInterfacesType();
	List<IosNetworkInterface> interfaces = IosNetworkInterface.getInterfaces(techSupport);
	for (IosNetworkInterface intf : interfaces) {
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
	return info;
    }
}
