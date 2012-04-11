// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.sysinfo;

import java.util.List;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.juniper.system.ISupportInformation;
import org.joval.intf.juniper.system.IJunosSession;
import org.joval.oval.Factories;

/**
 * Tool for creating a SystemInfoType from a Juniper JunOS-attached ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class JunosSystemInfo {
    static SystemInfoType getSystemInfo(IJunosSession session) throws NoSuchElementException {
	ISupportInformation supportInfo = session.getSupportInformation();
	SystemInfoType info = Factories.sc.core.createSystemInfoType();
	info.setOsName("Juniper JunOS");
	for (String line : supportInfo.getData("show version detail")) {
	    if (line.startsWith("Hostname:")) {
		info.setPrimaryHostName(line.substring(9).trim());
	    } else if (line.startsWith("Model:")) {
		info.setArchitecture(line.substring(6).trim());
	    } else if (line.startsWith("JUNOS Base OS Software Suite")) {
		int ptr = line.indexOf("[");
		if (ptr != -1) {
		    int begin = ptr + 1;
		    int end = line.indexOf("]", begin+1);
		    if (end == -1) {
			info.setOsVersion(line.substring(begin));
		    } else {
			info.setOsVersion(line.substring(begin, end));
		    }
		}
	    }
	}

	InterfacesType interfacesType = Factories.sc.core.createInterfacesType();
	List<JunosNetworkInterface> interfaces = JunosNetworkInterface.getInterfaces(supportInfo);
	for (JunosNetworkInterface intf : interfaces) {
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
