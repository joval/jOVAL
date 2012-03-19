// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.juniper;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.NoSuchElementException;

import org.joval.intf.cisco.system.ITechSupport;

/**
 * Tool for creating Network Interface information from an ISession attached to a JunOS device.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class JunosNetworkInterface {
    static List<JunosNetworkInterface> getInterfaces(ITechSupport techSupport) throws NoSuchElementException {
	Vector<JunosNetworkInterface> interfaces = new Vector<JunosNetworkInterface>();
	Vector<String> lines = new Vector<String>();
	String mac = null;
	for (String line : techSupport.getData("show interfaces extensive")) {
	    line = line.trim();
	    if (line.startsWith("Physical interface")) {
		mac = null;
	    } else if (line.startsWith("Current address")) {
		int begin = line.indexOf(":") + 1;
		if (begin > 0) {
		    int end = line.indexOf(",", begin);
		    if (end > begin) {
			mac = line.substring(begin, end).trim();
		    }
		}
	    } else if (line.startsWith("Logical interface")) {
		if (lines.size() > 0) {
		    interfaces.add(createJunosInterface(lines, mac));
		}
		lines = new Vector<String>();
		lines.add(line);
	    } else if (line.length() == 0) {
		if (lines.size() > 0) {
		    interfaces.add(createJunosInterface(lines, mac));
		}
		lines = new Vector<String>();
	    } else if (lines.size() > 0) {
		lines.add(line);
	    }
	}
	if (lines.size() > 0) {
	    interfaces.add(createJunosInterface(lines, mac));
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

    private JunosNetworkInterface(String mac, String ip4, String ip6, String description) {
	this.mac = mac;
	this.ip4 = ip4;
	this.ip6 = ip6;
	this.description = description;
    }

    private static JunosNetworkInterface createJunosInterface(Vector<String> lines, String mac) {
	String ip4=null, ip6=null, description="";
	String firstLine = lines.get(0);

	StringTokenizer tok = new StringTokenizer(firstLine);
	tok.nextToken();
	tok.nextToken();
	description = tok.nextToken();
System.out.println("DAS new interface " + description + ", mac=" +mac);
	if (lines.size() > 1) {
	    for (int i=1; i < lines.size(); i++) {
		String line = lines.get(i).trim();
		if (line.startsWith("Protocol inet")) {
		    String ipaddr = null;
		    for (int j=i+1; j < lines.size(); j++) {
			String line2 = lines.get(j).trim();
			if (line2.startsWith("Protocol")) {
			    break;
			} else if (line2.startsWith("Destination")) {
			    StringTokenizer tok2 = new StringTokenizer(line2);
			    while(tok2.hasMoreTokens()) {
				String token = tok2.nextToken();
				if (token.equals("Local:")) {
				    ipaddr = tok2.nextToken();
				    ipaddr = ipaddr.substring(0, ipaddr.length() - 1);
				    break;
				}
			    }
			}
		    }
		    if (ipaddr != null) {
			if (line.startsWith("Protocol inet6")) {
			    ip6 = ipaddr;
			} else {
			    ip4 = ipaddr;
			}
		    }
		}
	    }
	}
	return new JunosNetworkInterface(mac, ip4, ip6, description);
    }
}
