// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALSystem;

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

	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	try {
	    IProcess p = session.createProcess("hostname");
	    p.start();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    info.setPrimaryHostName(reader.readLine());
	    reader.close();

	    p = session.createProcess("uname -r");
	    p.start();
	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    info.setOsVersion(reader.readLine());
	    reader.close();

	    IFilesystem fs = session.getFilesystem();
	    for (INode node : fs.getFile("/etc").getChildren(Pattern.compile("^.*-release$"))) {
		p = session.createProcess("cat " + node.getPath());
		p.start();
    		reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		info.setOsName(reader.readLine());
		reader.close();
	    }
	    if (!info.isSetOsName()) {
		info.setOsName(session.getFlavor().getOsName());
	    }

	    p = session.createProcess("uname -p");
	    p.start();
    	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    info.setArchitecture(reader.readLine());
	    reader.close();

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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PLUGIN_INTERFACE"), e);
	}
	return info;
    }
}
