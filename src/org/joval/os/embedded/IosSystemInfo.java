// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from a Cisco IOS-attached ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IosSystemInfo {
    private ISession session;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public IosSystemInfo(ISession session) {
	this.session = session;
    }

    public SystemInfoType getSystemInfo() {
	if (info != null) {
	    return info;
	}

	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	info.setOsName("Cisco IOS");
	try {
	    IProcess p = session.createProcess("show config");
	    p.start();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		if (line.startsWith("hostname")) {
		    info.setPrimaryHostName(line.substring(9).trim());
		}
	    }
	    reader.close();
	    p.waitFor(0);

	    p = session.createProcess("show version");
	    p.start();
	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    while ((line = reader.readLine()) != null) {
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
	    List<IosNetworkInterface> interfaces = IosNetworkInterface.getInterfaces(session);
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
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_INTERFACE);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return info;
    }
}
