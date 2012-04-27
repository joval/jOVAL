// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.util.StringTokenizer;

import org.joval.intf.net.ICIDR;
import org.joval.intf.oval.IType;
import org.joval.net.Ip6Address;

/**
 * A type class for dealing with individual addresses or CIDR ranges for IPv6.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Ip6AddressType extends AbstractType implements ICIDR<Ip6AddressType> {
    private Ip6Address addr;

    public Ip6AddressType(String str) throws IllegalArgumentException {
	addr = new Ip6Address(str);
    }

    public String getData() {
	return addr.toString();
    }

    // Implement IType

    public Type getType() {
	return Type.IPV_6_ADDRESS;
    }

    public String getString() {
	return addr.toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	Ip6AddressType other = null;
	try {
	    other = (Ip6AddressType)t.cast(getType());
	} catch (UnsupportedOperationException e) {
	    throw new IllegalArgumentException(e);
	}
	if (toString().equals(other.toString())) {
	    return 0;
	} else if (addr.contains(other.addr)) {
	    return 1;
	} else {
	    return -1;
	}
    }

    // Implement ICIDR

    public boolean contains(Ip6AddressType other) {
	return addr.contains(other.addr);
    }
}
