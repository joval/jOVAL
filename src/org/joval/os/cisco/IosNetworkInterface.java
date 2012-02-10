// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.cisco;

import java.util.List;
import java.util.Vector;
import java.util.NoSuchElementException;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating Network Interface information from an ISession attached to an IOS device.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class IosNetworkInterface {
    static List<IosNetworkInterface> getInterfaces(ITechSupport techSupport) throws NoSuchElementException {
	Vector<IosNetworkInterface> interfaces = new Vector<IosNetworkInterface>();
	Vector<String> lines = new Vector<String>();
	for (String line : techSupport.getData("show interfaces")) {
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
