// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.io.InputStream;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

/**
 * Tool for creating a SystemInfoType from an IUnixSession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class UnixNetworkInterface {
    static List<UnixNetworkInterface> getInterfaces(IUnixSession session) throws Exception {
	Vector<UnixNetworkInterface> interfaces = new Vector<UnixNetworkInterface>();
	Vector<String> lines = new Vector<String>();
	List<String> rawOutput = null;
	switch(session.getFlavor()) {
	  case AIX:
	    rawOutput = SafeCLI.multiLine("/etc/ifconfig -a", session, IUnixSession.Timeout.S);
	    break;
	  default:
	    rawOutput = SafeCLI.multiLine("/sbin/ifconfig -a", session, IUnixSession.Timeout.S);
	    break;
	}

	switch(session.getFlavor()) {
	  //
	  // On Mac OS X, new interfaces start at char0 on a line.  Continutaion info starts with a tab after the first line.
	  // Interfaces are not separated by blank lines.
	  //
	  case MACOSX:
	    for (String line : rawOutput) {
		if (line.startsWith("\t") || line.startsWith("  ")) {
		    lines.add(line);
		} else if (lines.size() > 0) {
		    interfaces.add(createDarwinInterface(lines));
		    lines = new Vector<String>();
		    if (line.trim().length() > 0) {
			lines.add(line);
		    }
		} else if (line.trim().length() > 0) {
		    lines.add(line);
		}
	    }
	    if (lines.size() > 0) {
		interfaces.add(createDarwinInterface(lines));
	    }
	    break;

	  //
	  // On AIX and Solaris, new interfaces start at char0 on a line.  Continutaion info starts with a tab after the first
	  // line.  Interfaces are not separated by blank lines.
	  //
	  case AIX:
	  case SOLARIS:
	    for (String line : rawOutput) {
		if (line.startsWith("\t") || line.startsWith("  ")) {
		    lines.add(line);
		} else if (lines.size() > 0) {
		    interfaces.add(createUnixInterface(lines));
		    lines = new Vector<String>();
		    if (line.trim().length() > 0) {
			lines.add(line);
		    }
		} else if (line.trim().length() > 0) {
		    lines.add(line);
		}
	    }
	    if (lines.size() > 0) {
		interfaces.add(createUnixInterface(lines));
	    }
	    break;

	  //
	  // On Linux, there is a blank line between each interface spec.
	  //
	  case LINUX:
	    for (String line : rawOutput) {
		if (line.trim().length() == 0) {
		    if (lines.size() > 0) {
			interfaces.add(createLinuxInterface(lines));
			lines = new Vector<String>();
		    }
		} else {
		    lines.add(line);
		}
	    }
	    break;
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

    private UnixNetworkInterface(String mac, String ip4, String ip6, String description) {
	this.mac = mac;
	this.ip4 = ip4;
	this.ip6 = ip6;
	this.description = description;
    }

    private static UnixNetworkInterface createDarwinInterface(Vector<String> lines) {
	String mac="", ip4=null, ip6=null, description="";
	String firstLine = lines.get(0);
	description = firstLine.substring(0, firstLine.indexOf(":"));

	for (int i=1; i < lines.size(); i++) {
	    String line = lines.get(i).trim();
	    StringTokenizer tok = new StringTokenizer(line);
	    String token = tok.nextToken().toUpperCase();
	    if ("INET6".equals(token) && ip6 == null) {
		String temp = tok.nextToken();
		int ptr = temp.indexOf("%");
		if (ptr == -1) {
		    ip6 = temp;
		} else {
		    ip6 = temp.substring(0, ptr);
		}
	    } else if ("INET".equals(token)) {
		ip4 = tok.nextToken();
	    } else if ("ETHER".equals(token)) {
		mac = tok.nextToken();
	    }
	}
	return new UnixNetworkInterface(mac, ip4, ip6, description);
    }

    private static UnixNetworkInterface createUnixInterface(Vector<String> lines) {
	String mac="", ip4=null, ip6=null, description="";
	String firstLine = lines.get(0);
	description = firstLine.substring(0, firstLine.indexOf(":"));
	for (int i=1; i < lines.size(); i++) {
	    String line = lines.get(i).trim();
	    StringTokenizer tok = new StringTokenizer(line);
	    String addressType = tok.nextToken().toUpperCase();
	    if ("INET6".equals(addressType)) {
		String temp = tok.nextToken();
		ip6 = temp.substring(0, temp.indexOf("/"));
	    } else if ("INET".equals(addressType)) {
		ip4 = tok.nextToken();
	    } else if ("ETHER".equals(addressType)) {
		mac = tok.nextToken();
	    }
	}
	return new UnixNetworkInterface(mac, ip4, ip6, description);
    }

    private static UnixNetworkInterface createLinuxInterface(Vector<String> lines) {
	String mac="", ip4=null, ip6=null, description="";
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
	return new UnixNetworkInterface(mac, ip4, ip6, description);
    }
}
