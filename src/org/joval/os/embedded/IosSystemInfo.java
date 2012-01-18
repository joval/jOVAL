// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded;

import java.util.List;
import java.util.NoSuchElementException;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from a Cisco IOS-attached ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosSystemInfo {
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public IosSystemInfo(ITechSupport techSupport) throws NoSuchElementException {
	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	info.setOsName("Cisco IOS");
	    for (String line : techSupport.getData("show running-config")) {
		if (line.startsWith("hostname")) {
		    info.setPrimaryHostName(line.substring(9).trim());
		    break;
		}
	    }

	    for (String line : techSupport.getData("show version")) {
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

	    InterfacesType interfacesType = JOVALSystem.factories.sc.core.createInterfacesType();
	    List<IosNetworkInterface> interfaces = IosNetworkInterface.getInterfaces(techSupport);
	    for (IosNetworkInterface intf : interfaces) {
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
    }

    public SystemInfoType getSystemInfo() {
	return info;
    }
}
