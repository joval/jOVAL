// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.sysinfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

/**
 * Tool for creating a SystemInfoType from an IUnixSession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixNetworkInterface {
    public static List<UnixNetworkInterface> getInterfaces(IUnixSession session) throws Exception {
	ArrayList<UnixNetworkInterface> interfaces = new ArrayList<UnixNetworkInterface>();
	ArrayList<String> lines = new ArrayList<String>();
	List<String> rawOutput = null;
	switch(session.getFlavor()) {
	  case AIX: {
	    rawOutput = new ArrayList<String>();
	    Iterator<String> iter = SafeCLI.manyLines("/etc/ifconfig -a", null, session);
	    while(iter.hasNext()) {
		rawOutput.add(iter.next());
	    }
	    break;
	  }
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
		    lines = new ArrayList<String>();
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
		    interfaces.add(createUnixInterface(lines, session.getFlavor()));
		    lines = new ArrayList<String>();
		    if (line.trim().length() > 0) {
			lines.add(line);
		    }
		} else if (line.trim().length() > 0) {
		    lines.add(line);
		}
	    }
	    if (lines.size() > 0) {
		interfaces.add(createUnixInterface(lines, session.getFlavor()));
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
			lines = new ArrayList<String>();
		    }
		} else {
		    lines.add(line);
		}
	    }
	    break;
	}

	return interfaces;
    }

    public enum Type {
	UNKNOWN, ETHER, LOOPBACK, PPP, SLIP, TOKENRING;
    }

    public Type getType() {
	return type;
    }

    public String getName() {
	return name;
    }

    public String getHardwareAddress() {
	return hwaddr;
    }

    public Collection<IPAddress> getIPAddresses() {
	return addresses;
    }

    public Collection<String> getFlags() {
	return flags;
    }

    /**
     * Information about an IP address.
     */
    public static class IPAddress {
	private String addr, broadcast, mask;
	private Version version;

	/**
	 * IP versions
 	 */
	public enum Version {
	    V4, V6;
	}

	public Version getVersion() {
	    return version;
	}

	public String getAddress() {
	    return addr;
	}

	public String getBroadcast() {
	    return broadcast;
	}

	public String getMask() {
	    return mask;
	}

	// Internal

	IPAddress(Version version, String addr, String broadcast, String mask) {
	    this.version = version;
	    this.addr = addr;
	    if (version == Version.V4) {
		if (mask.indexOf(".") == -1) {
		    if (mask.startsWith("0x")) {
			mask = mask.substring(2);
		    }
		    StringBuffer sb = new StringBuffer();
		    for (int i=0; i < 4; i++) {
			int num = 0;
			int start = i * 2;
			int end = start + 2;
			if (end <= mask.length()) {
			    num = Integer.parseInt(mask.substring(start, end), 16);
			}
			if (i > 0) {
			    sb.append(".");
			}
			sb.append(Integer.toString(num));
		    }
		    this.mask = sb.toString();
		} else {
		    this.mask = mask;
		}
	    }
	    this.broadcast = broadcast;
	}
    }

    // Private

    private Type type;
    private String name, hwaddr;
    private Collection<String> flags;
    private Collection<IPAddress> addresses;

    private UnixNetworkInterface(Type type, String name, String hwaddr, Collection<IPAddress> addresses,
		Collection<String> flags) {

	this.type = type;
	this.name = name;
	this.hwaddr = hwaddr;
	this.addresses = addresses;
	this.flags = flags;
    }

    private static UnixNetworkInterface createDarwinInterface(ArrayList<String> lines) {
	Type type = Type.UNKNOWN;
	Collection<IPAddress> addresses = new ArrayList<IPAddress>();
	Collection<String> flags = new ArrayList<String>();
	String hwaddr=null, name=null;

	String firstLine = lines.get(0);
	name = firstLine.substring(0, firstLine.indexOf(":"));
	if (name.startsWith("lo")) {
	    type = Type.LOOPBACK;
	}

	int startFlags = firstLine.indexOf("<") + 1;
	int endFlags = firstLine.indexOf(">");
	if (startFlags < endFlags) {
	    StringTokenizer tok = new StringTokenizer(firstLine.substring(startFlags, endFlags), ",");
	    while (tok.hasMoreTokens()) {
		String flag = tok.nextToken();
		if ("POINTOPOINT".equals(flag)) {
		    type = Type.PPP;
		}
		flags.add(flag);
	    }
	}

	for (int i=1; i < lines.size(); i++) {
	    String line = lines.get(i).trim();
	    StringTokenizer tok = new StringTokenizer(line);
	    String addressType = tok.nextToken().toLowerCase();
	    if ("inet6".equals(addressType)) {
		String addr = tok.nextToken();
		String mask = null;
		int ptr = addr.indexOf("%");
		if (ptr != -1) {
		    addr = addr.substring(0,ptr);
		}
		while (tok.hasMoreTokens()) {
		    String s = tok.nextToken().toLowerCase();
		    if ("prefixlen".equals(s)) {
			mask = tok.nextToken();
		    }
		}
		addresses.add(new IPAddress(IPAddress.Version.V6, addr, null, mask));
	    } else if ("inet".equals(addressType)) {
		String addr = tok.nextToken();
		String broadcast=null, mask=null;
		while (tok.hasMoreTokens()) {
		    String s = tok.nextToken().toLowerCase();
		    if ("netmask".equals(s)) {
			mask = tok.nextToken();
		    } else if ("broadcast".equals(s)) {
			broadcast = tok.nextToken();
		    }
		}
		addresses.add(new IPAddress(IPAddress.Version.V4, addr, null, mask));
	    } else if ("ether".equals(addressType)) {
		if (type == Type.UNKNOWN) {
		    type = Type.ETHER;
		}
		hwaddr = tok.nextToken();
	    }
	}
	return new UnixNetworkInterface(type, name, hwaddr, addresses, flags);
    }

    private static UnixNetworkInterface createUnixInterface(ArrayList<String> lines, IUnixSession.Flavor flavor) {
	Type type = Type.UNKNOWN;
	Collection<IPAddress> addresses = new ArrayList<IPAddress>();
	Collection<String> flags = new ArrayList<String>();
	String hwaddr=null, name=null;

	String firstLine = lines.get(0);
	name = firstLine.substring(0,firstLine.indexOf(":"));
	if (name.startsWith("lo")) {
	    type = Type.LOOPBACK;
	}
	if (flavor == IUnixSession.Flavor.AIX) {
	    if (name.startsWith("en")) {
		type = Type.ETHER;
	    } else if (name.startsWith("pp")) {
		type = Type.PPP;
	    } else if (name.startsWith("tr")) {
		type = Type.TOKENRING;
	    } else if (name.startsWith("sl")) {
		type = Type.SLIP;
	    }
	}

	int startFlags = firstLine.indexOf("<") + 1;
	int endFlags = firstLine.indexOf(">");
	if (startFlags < endFlags) {
	    StringTokenizer tok = new StringTokenizer(firstLine.substring(startFlags, endFlags), ",");
	    while (tok.hasMoreTokens()) {
		String flag = tok.nextToken();
		if ("POINTOPOINT".equals(flag)) {
		    type = Type.PPP;
		}
		flags.add(flag);
	    }
	}

	for (int i=1; i < lines.size(); i++) {
	    String line = lines.get(i).trim();
	    StringTokenizer tok = new StringTokenizer(line);
	    String addressType = tok.nextToken().toLowerCase();
	    if ("inet6".equals(addressType)) {
		String cidr = tok.nextToken();
		int ptr = cidr.indexOf("/");
		String addr = cidr.substring(0,ptr);
		String mask = cidr.substring(ptr+1);
		ptr = addr.indexOf("%");
		if (ptr != -1) {
		    addr = addr.substring(0,ptr);
		}
		addresses.add(new IPAddress(IPAddress.Version.V6, addr, null, mask));
	    } else if ("inet".equals(addressType)) {
		String addr = tok.nextToken();
		String broadcast=null, mask=null;
		while (tok.hasMoreTokens()) {
		    String s = tok.nextToken().toLowerCase();
		    if ("netmask".equals(s)) {
			mask = tok.nextToken();
		    } else if ("broadcast".equals(s)) {
			broadcast = tok.nextToken();
		    }
		}
		addresses.add(new IPAddress(IPAddress.Version.V4, addr, broadcast, mask));
	    } else if ("ether".equals(addressType)) {
		if (type == Type.UNKNOWN) {
		    type = Type.ETHER;
		}
		hwaddr = tok.nextToken();
	    }
	}
	return new UnixNetworkInterface(type, name, hwaddr, addresses, flags);
    }

    private static UnixNetworkInterface createLinuxInterface(ArrayList<String> lines) {
	Type type = Type.UNKNOWN;
	Collection<IPAddress> addresses = new ArrayList<IPAddress>();
	Collection<String> flags = new ArrayList<String>();
	String hwaddr=null, name=null;

	String line1 = lines.get(0);
	StringTokenizer tok = new StringTokenizer(line1);
	name = tok.nextToken();
	if (line1.toLowerCase().indexOf("loopback") != -1) {
	    type = Type.LOOPBACK;
	} else if (line1.toLowerCase().indexOf("ethernet") != -1) {
	    type = Type.ETHER;
	} else if (line1.toLowerCase().indexOf("point-point protocol") != -1) {
	    type = Type.PPP;
	}

	while(tok.hasMoreTokens()) {
	    if ("HWaddr".equalsIgnoreCase(tok.nextToken()) && tok.hasMoreTokens()) {
		hwaddr = tok.nextToken();
	    }
	}

	if (lines.size() > 1) {
	    String line2 = lines.get(1).trim();
	    if (line2.toLowerCase().startsWith("inet addr:")) {
		tok = new StringTokenizer(line2.substring(10));
		String addr = tok.nextToken();
		String broadcast=null, mask=null;
		while (tok.hasMoreTokens()) {
		    String s = tok.nextToken();
		    if (s.toLowerCase().startsWith("bcast:")) {
			broadcast = s.substring(6);
		    } else if (s.toLowerCase().startsWith("mask:")) {
			mask = s.substring(5);
		    }
		}
		addresses.add(new IPAddress(IPAddress.Version.V4, addr, broadcast, mask));
	    }
	}
	if (lines.size() > 2) {
	    String line3 = lines.get(2).trim();
	    if (line3.toLowerCase().startsWith("inet6 addr:")) {
		tok = new StringTokenizer(line3.substring(11));
		String s = tok.nextToken();
		int ptr = s.indexOf("/");
		String addr=null, mask=null;
		if (ptr == -1) {
		    addr = s;
		} else {
		    addr = s.substring(0,ptr);
		    mask = s.substring(ptr+1);
		}
		addresses.add(new IPAddress(IPAddress.Version.V6, addr, null, mask));
	    }
	}

	// the next line after addresses contains the flags
	int flagLineNum = 1 + addresses.size();
	if (lines.size() > flagLineNum) {
	    String s = lines.get(flagLineNum).trim();
	    int ptr = s.indexOf("  "); // 2 spaces indicates end of flags
	    if (ptr != -1) {
		s = s.substring(0,ptr);
	    }
	    tok = new StringTokenizer(s);
	    while (tok.hasMoreTokens()) {
		flags.add(tok.nextToken());
	    }
	}
	return new UnixNetworkInterface(type, name, hwaddr, addresses, flags);
    }
}
