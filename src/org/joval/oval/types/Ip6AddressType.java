// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.util.StringTokenizer;

import org.joval.intf.net.ICIDR;
import org.joval.intf.oval.IType;

/**
 * A utility class for dealing with individual addresses or CIDR ranges for IPv6.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Ip6AddressType extends AbstractType implements ICIDR<Ip6AddressType> {
    private short[] addr = new short[16];
    private short[] mask = new short[16];
    private int maskVal;

    public Ip6AddressType(String str) throws IllegalArgumentException {
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

    public String getData() {
	return toString();
    }

    // Implement IType

    public Type getType() {
	return Type.IPV_6_ADDRESS;
    }

    public String getString() {
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

    public int compareTo(IType t) {
	Ip6AddressType other = null;
	try {
	    other = (Ip6AddressType)t.cast(getType());
	} catch (UnsupportedOperationException e) {
	    throw new IllegalArgumentException(e);
	}
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

    // Implement ICIDR

    public boolean contains(Ip6AddressType other) {
	for (int i=0; i < 16; i++) {
	    if (addr[i] != (other.addr[i] & mask[i])) {
		return false;
	    }
	}
	return true;
    }
}
