// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from a Unix ISession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSystemInfo {
    private ISession session;
    private ObjectFactory coreFactory;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public UnixSystemInfo(ISession session) {
	coreFactory = new ObjectFactory();
	this.session = session;
    }

    public SystemInfoType getSystemInfo() {
	if (info != null) {
	    return info;
	}

	info = coreFactory.createSystemInfoType();
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
	    List<String> releaseFiles = fs.search("^/etc/.*-release$");
	    if (releaseFiles.size() > 0) {
		p = session.createProcess("cat " + releaseFiles.get(0));
	    } else {
		p = session.createProcess("uname -s");
	    }
	    p.start();
    	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    info.setOsName(reader.readLine());
	    reader.close();

	    p = session.createProcess("uname -p");
	    p.start();
    	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    info.setArchitecture(reader.readLine());
	    reader.close();

	    Vector<Interface> interfaces = new Vector<Interface>();
	    Vector<String> lines = new Vector<String>();
	    String line = null;
	    p = session.createProcess("/sbin/ifconfig -a");
	    p.start();
    	    reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    while ((line = reader.readLine()) != null) {
		if (line.trim().length() == 0) {
		    if (lines.size() > 0) {
			interfaces.add(new Interface(lines));
			lines = new Vector<String>();
		    }
		} else {
		    lines.add(line);
		}
	    }
	    reader.close();
	    
	    InterfacesType interfacesType = coreFactory.createInterfacesType();
	    Iterator<Interface>iter = interfaces.iterator();
	    while (iter.hasNext()) {
		Interface intf = iter.next();
		InterfaceType interfaceType = coreFactory.createInterfaceType();
		interfaceType.setMacAddress(intf.getMacAddress());
		interfaceType.setInterfaceName(intf.getDescription());
		EntityItemIPAddressStringType ipAddressType = coreFactory.createEntityItemIPAddressStringType();
		ipAddressType.setValue(intf.getIpV4Address());
		interfaceType.setIpAddress(ipAddressType);
		interfacesType.getInterface().add(interfaceType);
	    }
	    info.setInterfaces(interfacesType);
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PLUGIN_INTERFACE"), e);
	}
	return info;
    }

    // Private

    class Interface {
	String mac, ip4, ip6, description;

	Interface(Vector<String> lines) {
	    StringTokenizer tok = new StringTokenizer(lines.get(0));
	    description = tok.nextToken();
	    while(tok.hasMoreTokens()) {
		if ("HWaddr".equalsIgnoreCase(tok.nextToken()) && tok.hasMoreTokens()) {
		    mac = tok.nextToken();
		}
	    }
	    if (lines.size() > 1) {
		String line2 = lines.get(1).trim();
		if (line2.toUpperCase().startsWith("INET ADDR:")) {
		    tok = new StringTokenizer(line2.substring(10));
		    ip4 = tok.nextToken();
		}
	    }
	    if (lines.size() > 2) {
		String line3 = lines.get(2).trim();
		if (line3.toUpperCase().startsWith("INET6 ADDR:")) {
		    tok = new StringTokenizer(line3.substring(11));
		    ip6 = tok.nextToken();
		}
	    }
	    if (mac == null) {
		mac = "";
	    }
	    if (ip4 == null) {
		ip4 = "";
	    }
	    if (ip6 == null) {
		ip6 = "";
	    }
	}

	String getMacAddress() {
	    return mac;
	}

	String getIpV4Address() {
	    return ip4;
	}

	String getIpV6Address() {
	    return ip6;
	}

	String getDescription() {
	    return description;
	}
    }
}
