// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;

/**
 * Tool for creating a SystemInfoType from an IUnixSession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class NetworkInterface {
    static List<NetworkInterface> getInterfaces(IUnixSession session) throws Exception {
	Vector<NetworkInterface> interfaces = new Vector<NetworkInterface>();

	IProcess p = session.createProcess("/sbin/ifconfig -a", UnixSystemInfo.TIMEOUT, UnixSystemInfo.DEBUG);
	p.start();
	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	Vector<String> lines = new Vector<String>();
	String line = null;

	switch(session.getFlavor()) {
	  //
	  // On Mac OS X, new interfaces start at char0 on a line.  Continutaion info starts with a tab after the first line.
	  // Interfaces are not separated by blank lines.
	  //
	  case MACOSX:
	    while ((line = reader.readLine()) != null) {
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
	  // On Solaris, new interfaces start at char0 on a line.  Continutaion info starts with a tab after the first line.
	  // Interfaces are not separated by blank lines.
	  //
	  case SOLARIS:
	    while ((line = reader.readLine()) != null) {
		if (line.startsWith("\t") || line.startsWith("  ")) {
		    lines.add(line);
		} else if (lines.size() > 0) {
		    interfaces.add(createSolarisInterface(lines));
		    lines = new Vector<String>();
		    if (line.trim().length() > 0) {
			lines.add(line);
		    }
		} else if (line.trim().length() > 0) {
		    lines.add(line);
		}
	    }
	    if (lines.size() > 0) {
		interfaces.add(createSolarisInterface(lines));
	    }
	    break;

	  //
	  // On Linux, there is a blank line between each interface spec.
	  //
	  case LINUX:
	    while ((line = reader.readLine()) != null) {
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

    private NetworkInterface(String mac, String ip4, String ip6, String description) {
	this.mac = mac;
	this.ip4 = ip4;
	this.ip6 = ip6;
	this.description = description;
    }

    private static NetworkInterface createDarwinInterface(Vector<String> lines) {
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
	return new NetworkInterface(mac, ip4, ip6, description);
    }

    private static NetworkInterface createSolarisInterface(Vector<String> lines) {
	String mac="", ip4=null, ip6=null, description="";
	String firstLine = lines.get(0);
	description = firstLine.substring(0, firstLine.indexOf(":"));
	if (lines.size() > 1) {
	    String line2 = lines.get(1).trim();
	    StringTokenizer tok = new StringTokenizer(line2);
	    String addressType = tok.nextToken().toUpperCase();
	    if ("INET6".equals(addressType)) {
		String temp = tok.nextToken();
		ip6 = temp.substring(0, temp.indexOf("/"));
	    } else if ("INET".equals(addressType)) {
		ip4 = tok.nextToken();
	    }
	}
	if (lines.size() > 2) {
	    String line3 = lines.get(2).trim();
	    StringTokenizer tok = new StringTokenizer(line3);
	    String token = tok.nextToken().toUpperCase();
	    if ("ETHER".equals(token)) {
		mac = tok.nextToken();
	    }
	}
	return new NetworkInterface(mac, ip4, ip6, description);
    }

    private static NetworkInterface createLinuxInterface(Vector<String> lines) {
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
	return new NetworkInterface(mac, ip4, ip6, description);
    }
}
