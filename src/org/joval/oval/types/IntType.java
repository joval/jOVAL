// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import java.math.BigInteger;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Int type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IntType implements IType<IntType> {
    BigInteger data;

    public IntType(String data) throws NumberFormatException {
	this(new BigInteger(data));
    }

    public IntType(BigInteger data) {
	this.data = data;
    }

    public BigInteger getData() {
	return data;
    }

    public String toString() {
	return data.toString();
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.INT;
    }

    // Implement Comparable

    public int compareTo(IntType other) {
	return data.compareTo(other.data);
    }
}
