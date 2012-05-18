// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.math.BigInteger;

import org.joval.intf.oval.IType;

/**
 * Int type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IntType extends AbstractType {
    private BigInteger data;

    public IntType(String data) throws NumberFormatException {
	this(new BigInteger(data));
    }

    public IntType(BigInteger data) {
	this.data = data;
    }

    public BigInteger getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.INT;
    }

    public String getString() {
	return data.toString();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	IntType other = null;
	try {
	    other = (IntType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return data.compareTo(other.data);
    }
}
