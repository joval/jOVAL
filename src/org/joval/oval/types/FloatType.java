// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import org.joval.intf.oval.IType;

/**
 * Float type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FloatType extends AbstractType {
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

    public Float getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.FLOAT;
    }

    public String getString() {
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

    // Implement Comparable

    public int compareTo(IType t) {
	FloatType other = null;
	try {
	    other = (FloatType)t.cast(getType());
	} catch (UnsupportedOperationException e) {
	    throw new IllegalArgumentException(e);
	}
	return data.compareTo(other.data);
    }
}
