// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.juniper;

import java.util.List;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from a Juniper JunOS-attached ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JunosSystemInfo {
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public JunosSystemInfo(ITechSupport techSupport) throws NoSuchElementException {
	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	info.setOsName("Juniper JunOS");
	    for (String line : techSupport.getData("show version detail")) {
		if (line.startsWith("Hostname:")) {
		    info.setPrimaryHostName(line.substring(9).trim());
		} else if (line.startsWith("Model:")) {
		    info.setArchitecture(line.substring(6).trim());
		} else if (line.startsWith("JUNOS Base OS Software Suite")) {
		    int ptr = line.indexOf("[");
		    if (ptr != -1) {
			int begin = ptr + 8;
			int end = line.indexOf("]", begin+1);
			if (end == -1) {
			    info.setOsVersion(line.substring(begin));
			} else {
			    info.setOsVersion(line.substring(begin, end));
			}
		    }
		}
	    }

	    InterfacesType interfacesType = JOVALSystem.factories.sc.core.createInterfacesType();
	    List<JunosNetworkInterface> interfaces = JunosNetworkInterface.getInterfaces(techSupport);
	    for (JunosNetworkInterface intf : interfaces) {
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
