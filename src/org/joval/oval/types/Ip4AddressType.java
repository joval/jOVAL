// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.util.StringTokenizer;

import org.joval.intf.net.ICIDR;
import org.joval.intf.oval.IType;

/**
 * A utility class for dealing with individual addresses or CIDR ranges for IPv4.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Ip4AddressType extends AbstractType implements ICIDR<Ip4AddressType> {
    private short[] addr = new short[4];
    private short[] mask = new short[4];
    private int maskVal;

    public Ip4AddressType(String str) throws IllegalArgumentException {
	int ptr = str.indexOf("/");
	maskVal = 32;
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
	short[] maskBits = new short[32];
	for (int i=0; i < 32; i++) {
	    if (i < maskVal) {
		maskBits[i] = 1;
	    } else {
		maskBits[i] = 0;
	    }
	}
	for (int i=0; i < 4; i++) {
	    mask[i] = (short)(maskBits[8*i + 1] * 8 +
			      maskBits[8*i + 2] * 4 +
			      maskBits[8*i + 3] * 2 +
			      maskBits[8*i + 4]);
	}

	StringTokenizer tok = new StringTokenizer(ipStr, ".");
	int numTokens = tok.countTokens();
	if (numTokens > 4) {
	    throw new IllegalArgumentException(str);
	}
	for (int i=0; i < 4 - numTokens; i++) {
	    addr[i] = 0;
	}
	for (int i = 4 - tok.countTokens(); tok.hasMoreTokens(); i++) {
	    addr[i] = (short)(Short.parseShort(tok.nextToken()) & mask[i]);
	}
    }

    public String getData() {
	return toString();
    }

    // Implement IType

    public Type getType() {
	return Type.IPV_4_ADDRESS; 
    }

    public String getString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < 4; i++) {
	    if (i > 0) {
		sb.append(".");
	    }
	    sb.append(Short.toString(addr[i]));
	}
	return sb.append("/").append(Integer.toString(maskVal)).toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	Ip4AddressType other = null;
	try {
	    other = (Ip4AddressType)t.cast(getType());
	} catch (UnsupportedOperationException e) {
	    throw new IllegalArgumentException(e);
	}
	for (int i=0; i < 4; i++) {
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

    public boolean contains(Ip4AddressType other) {
	for (int i=0; i < 4; i++) {
	    if (addr[i] != (other.addr[i] & mask[i])) {
		return false;
	    }
	}
	return true;
    }
}
