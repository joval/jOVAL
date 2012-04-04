// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Float type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FloatType implements IType<FloatType> {
    Float data;

    public FloatType(String data) throws NumberFormatException {
	String s = data.trim();
	if (s.equals("NaN")) {
	    this.data = new Float(Float.NaN);
	} else if (s.equals("INF")) {
	    this.data = new Float(Float.POSITIVE_INFINITY);
	} else if (s.equals("-INF")) {
	    this.data = new Float(Float.NEGATIVE_INFINITY);
	} else {
	    this.data = new Float(s);
	}
    }

    public FloatType(Float data) {
	this.data = data;
    }

    public String toString() {
	if (data.equals(Float.POSITIVE_INFINITY)) {
	    return "INF";
	} else if (data.equals(Float.NEGATIVE_INFINITY)) {
	    return "-INF";
	} else if (data.equals(Float.NaN)) {
	    return "NaN";
	} else {
	    return data.toString();
	}
    }

    public Float getData() {
	return data;
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.FLOAT;
    }

    // Implement Comparable

    public int compareTo(FloatType other) {
	return data.compareTo(other.data);
    }
}
