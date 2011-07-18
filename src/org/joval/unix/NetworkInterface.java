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
class NetworkInterface {
    static List<NetworkInterface> getInterfaces(UnixFlavor flavor, ISession session) throws Exception {
	Vector<NetworkInterface> interfaces = new Vector<NetworkInterface>();

	IProcess p = session.createProcess("/sbin/ifconfig -a");
	p.start();
	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	Vector<String> lines = new Vector<String>();
	String line = null;

	switch(flavor) {
	  //
	  // On Solaris, info for every interface starts at char[0] of a line; with additional lines tab-indented
	  //
	  case SOLARIS:
	    while ((line = reader.readLine()) != null) {
		if (line.startsWith("\t")) {
		    lines.add(line);
		} else if (lines.size() > 0) {
		    interfaces.add(createSolarisInterface(lines));
		    lines = new Vector<String>();
		    lines.add(line);
		} else {
		    lines.add(line);
		}
	    }
	    if (lines.size() > 0) {
		interfaces.add(createSolarisInterface(lines));
	    }
	    break;

	  //
	  // On Linux, info for every interface ends with a blank line.
	  //
	  default:
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
