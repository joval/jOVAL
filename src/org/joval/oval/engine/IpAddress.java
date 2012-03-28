// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.util.StringTokenizer;

/**
 * A utility class for dealing with individual addresses or CIDR ranges for IPv4 and IPv6.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class IpAddress implements Comparable<IpAddress> {
    private short[] addr = new short[16];
    private short[] mask = new short[16];
    private int maskVal;

    IpAddress (String str) throws IllegalArgumentException {
	int ptr = str.indexOf("/");
	maskVal = 128;
	String ipStr = null;
	if (ptr == -1) {
	    ipStr = str;
	} else {
	    maskVal = Integer.parseInt(str.substring(ptr+1));
	    ipStr = str.substring(0, ptr);
	}

	//
	// Create the netmask
	//
	short[] maskBits = new short[128];
	for (int i=0; i < 128; i++) {
	    if (i < maskVal) {
		maskBits[i] = 1;
	    } else {
		maskBits[i] = 0;
	    }
	}
	for (int i=0; i < 16; i++) {
	    mask[i] = (short)(maskBits[8*i + 0] * 128 +
			      maskBits[8*i + 1] * 64 +
			      maskBits[8*i + 2] * 32 +
			      maskBits[8*i + 3] * 16 +
			      maskBits[8*i + 4] * 8 +
			      maskBits[8*i + 5] * 4 +
			      maskBits[8*i + 6] * 2 +
			      maskBits[8*i + 7]);
	}

	if (ipStr.indexOf(":") == -1) {
	    // Convert 32-bit to 128-bit mask
	    if (maskVal != 128) {
		maskVal += 96;
		for (int i=0; i < 12; i++) {
		    if (i < 4) {
			mask[12+i] = mask[i];
		    }
		    mask[i] = 255;
		}
	    }
	    // process as ipv4 address
	    StringTokenizer tok = new StringTokenizer(ipStr, ".");
	    for (int i=0; i < 16 - tok.countTokens(); i++) {
		addr[i] = 0;
	    }
	    for (int i = 16 - tok.countTokens(); tok.hasMoreTokens(); i++) {
		addr[i] = (short)(Short.parseShort(tok.nextToken()) & mask[i]);
	    }
	} else {
	    // process as ipv6 address
	    ptr = ipStr.indexOf("::");
	    String prefix = null;
	    String suffix = null;
	    if (ptr != -1) {
		if (!ipStr.startsWith("::")) {
		    prefix = ipStr.substring(0,ptr);
		}
		if (!ipStr.endsWith("::")) {
		    suffix = ipStr.substring(ptr+1);
		}
	    } else {
		prefix = ipStr;
	    }
	    String[] octets = new String[8];
	    for (int i=0; i < 8; i++) {
		octets[i] = "0";
	    }
	    if (prefix != null) {
		StringTokenizer tok = new StringTokenizer(prefix, ":");
		for (int i=0; tok.hasMoreTokens(); i++) {
		    octets[i] = tok.nextToken();
		}
	    }
	    if (suffix != null) {
		StringTokenizer tok = new StringTokenizer(suffix, ":");
		for (int i = 8 - tok.countTokens(); tok.hasMoreTokens(); i++) {
		    octets[i] = tok.nextToken();
		}
	    }
	    // convert octets to bytes
	    for (int i=0; i < 8; i++) {
		int addrIndex = i*2;
		int val = Integer.parseInt(octets[i], 16);
		addr[addrIndex] = (short)(((val >> 8) & 0xFF) & mask[addrIndex]);
		addrIndex++;
		addr[addrIndex] = (short)((val & 0xFF) & mask[addrIndex]);
	    }
	}
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0, j=0; i < 16; j++) {
	    if (i > 0) {
		sb.append(":");
	    }
	    StringBuffer octet = new StringBuffer();
	    octet.append(Integer.toHexString(addr[i++] & 0xFF));
	    octet.append(Integer.toHexString(addr[i++] & 0xFF));
	    sb.append(Integer.toHexString(Integer.parseInt(octet.toString(), 16)));
	}
	return sb.append("/").append(Integer.toString(maskVal)).toString();
    }

    // Implement Comparable

    public int compareTo(IpAddress other) {
	for (int i=0; i < 16; i++) {
	    if (addr[i] == other.addr[i]) {
		continue;
	    } else if (addr[i] < other.addr[i]) {
		return -1;
	    } else {
		return 1;
	    }
	}
	return 0;
    }

    // Internal

    boolean contains(IpAddress other) {
	for (int i=0; i < 16; i++) {
	    if (addr[i] != (other.addr[i] & mask[i])) {
		return false;
	    }
	}
	return true;
    }
}
