// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.util.StringTokenizer;

import org.joval.intf.net.ICIDR;
import org.joval.intf.oval.IType;
import org.joval.net.Ip4Address;

/**
 * A type class for dealing with individual addresses or CIDR ranges for IPv4.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Ip4AddressType extends AbstractType implements ICIDR<Ip4AddressType> {
    private Ip4Address addr;

    public Ip4AddressType(String str) throws IllegalArgumentException {
	addr = new Ip4Address(str);
    }

    public String getData() {
	return addr.toString();
    }

    // Implement IType

    public Type getType() {
	return Type.IPV_4_ADDRESS; 
    }

    public String getString() {
	return addr.toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	Ip4AddressType other = null;
	try {
	    other = (Ip4AddressType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return addr.toBigInteger().compareTo(other.addr.toBigInteger());
    }

    // Implement ICIDR

    public boolean contains(Ip4AddressType other) {
	return addr.contains(other.addr);
    }
}
