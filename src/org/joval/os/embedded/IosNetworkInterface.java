// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.embedded;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

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
 * Tool for creating Network Interface information from an ISession attached to an IOS device.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class IosNetworkInterface {
    static List<IosNetworkInterface> getInterfaces(ISession session) throws Exception {
	Vector<IosNetworkInterface> interfaces = new Vector<IosNetworkInterface>();

	IProcess p = session.createProcess("show interfaces");
	p.start();
	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	Vector<String> lines = new Vector<String>();
	String line = null;
	while ((line = reader.readLine()) != null) {
	    if (line.startsWith(" ")) {
		lines.add(line);
	    } else if (lines.size() > 0) {
		interfaces.add(createIosInterface(lines));
		lines = new Vector<String>();
		if (line.trim().length() > 0) {
		    lines.add(line);
		}
	    } else if (line.trim().length() > 0) {
		lines.add(line);
	    }
	}
	if (lines.size() > 0) {
	    interfaces.add(createIosInterface(lines));
	}

	reader.close();
	return interfaces;
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

    // Private

    private String mac, ip4, ip6, description;

    private IosNetworkInterface(String mac, String ip4, String ip6, String description) {
	this.mac = mac;
	this.ip4 = ip4;
	this.ip6 = ip6;
	this.description = description;
    }

    private static IosNetworkInterface createIosInterface(Vector<String> lines) {
	String mac="", ip4=null, ip6=null, description="";
	String firstLine = lines.get(0);
	description = firstLine.substring(0, firstLine.indexOf(" is "));
	if (lines.size() > 1) {
	    for (int i=1; i < lines.size(); i++) {
		String line = lines.get(i);
		if (line.trim().startsWith("Hardware is ")) {
		    int ptr = line.indexOf(" address is ");
		    if (ptr > 0) {
			int begin = ptr + 12;
			int end = line.indexOf(" ", begin+1);
			if (end > 0) {
			    mac = line.substring(begin, end);
			} else {
			    mac = line.substring(begin);
			}
		    }
		} else if (line.trim().startsWith("Internet address is ")) {
		    ip4 = line.trim().substring(20);
		}
	    }
	}
	return new IosNetworkInterface(mac, ip4, ip6, description);
    }
}
