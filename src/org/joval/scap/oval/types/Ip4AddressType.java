// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import java.math.BigInteger;
import java.util.StringTokenizer;

import org.joval.intf.scap.oval.IType;

/**
 * A type class for dealing with individual addresses or CIDR ranges for IPv4.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Ip4AddressType extends AbstractType {
    private short[] addr = new short[4];
    private short[] mask = new short[4];
    private int maskVal;

    public Ip4AddressType(String str) throws IllegalArgumentException {
	str = str.trim();
	if (str.length() == 0) {
	    throw new IllegalArgumentException(str);
	}
	int ptr = str.indexOf("/");
	maskVal = 32;
	String ipStr = null;
	if (ptr == -1) {
	    ipStr = str;
	} else {
	    ipStr = str.substring(0, ptr);

	    String s = str.substring(ptr+1);
	    if (s.indexOf(".") == -1)  {
		//
		// Mask is expressed in the form of an integer, from 0-32
		//
		maskVal = Integer.parseInt(str.substring(ptr+1));
	    } else {
		//
		// Mask is expressed as a subnet, in octets
		//
		StringTokenizer tok = new StringTokenizer(".");
		if (tok.countTokens() > 4) {
		    throw new IllegalArgumentException(str);
		}
		maskVal = 0;
		while (tok.hasMoreTokens()) {
		    short n = (short)(Short.parseShort(tok.nextToken()));
		    if (128 == (n & 128)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (64 == (n & 64)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (32 == (n & 32)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (16 == (n & 16)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (8 == (n & 8)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (4 == (n & 4)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (2 == (n & 2)) {
			maskVal++;
		    } else {
			break;
		    }
		    if (1 == (n & 1)) {
			maskVal++;
		    } else {
			break;
		    }
		}
	    }
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
	    mask[i] = (short)(maskBits[8*i + 0] * 128 +
			      maskBits[8*i + 1] * 64 +
			      maskBits[8*i + 2] * 32 +
			      maskBits[8*i + 3] * 16 +
			      maskBits[8*i + 4] * 8 +
			      maskBits[8*i + 5] * 4 +
			      maskBits[8*i + 6] * 2 +
			      maskBits[8*i + 7]);
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

    public BigInteger toBigInteger() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < addr.length; i++) {
	    sb.append(Integer.toHexString(addr[i]));
	}
	return new BigInteger(sb.toString(), 16);
    }

    public String getIpAddressString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < 4; i++) {
	    if (i > 0) {
		sb.append(".");
	    }
	    sb.append(Short.toString(addr[i]));
	}
	return sb.toString();
    }

    public String getSubnetString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < 4; i++) {
	    if (i > 0) {
		sb.append(".");
	    }
	    sb.append(Short.toString(mask[i]));
	}
	return sb.toString();
    }

    public int getMask() {
	return maskVal;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer(getIpAddressString());
	return sb.append("/").append(Integer.toString(maskVal)).toString();
    }

    public boolean contains(Ip4AddressType other) {
	for (int i=0; i < 4; i++) {
	    if (addr[i] != (other.addr[i] & mask[i])) {
		return false;
	    }
	}
	return true;
    }

    // Implement IType

    public Type getType() {
	return Type.IPV_4_ADDRESS; 
    }

    public String getString() {
	return toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	Ip4AddressType other = null;
	try {
	    other = (Ip4AddressType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	if (getMask() == other.getMask()) {
	    return toBigInteger().compareTo(other.toBigInteger());
	} else {
	    throw new IllegalArgumentException(Integer.toString(other.getMask()));
	}
    }
}
