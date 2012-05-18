// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import org.joval.intf.oval.IType;

/**
 * String type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class StringType extends AbstractType {
    public final static StringType EMPTY = new StringType("");

    private String data;

    public StringType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    // Implement ITyped

    public Type getType() {
	return Type.STRING;
    }

    public String getString() {
	return data;
    }

    // Implement Comparable

    public int compareTo(IType t) {
	StringType other = null;
	try {
	    other = (StringType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	return data.compareTo(other.data);
    }
}
