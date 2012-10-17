// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import org.joval.intf.oval.IType;

/**
 * Binary type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BooleanType extends AbstractType {
    private boolean data;

    public BooleanType(String data) {
	if (data == null) {
	    this.data = false;
	} else if (data.equalsIgnoreCase("true")) {
	    this.data = true;
	} else if (data.equals("0")) {
	    this.data = false;
	} else if (data.equalsIgnoreCase("false")) {
	    this.data = false;
	} else if (data.length() == 0) {
	    this.data = false;
	} else {
	    this.data = true;
	}
    }

    public BooleanType(boolean data) {
	this.data = data;
    }

    public boolean getData() {
	return data;
    }

    // Implement IType

    public String getString() {
	return data ? "1" : "0";
    }

    public Type getType() {
	return Type.BOOLEAN;
    }

    // Implement Comparable

    public int compareTo(IType t) {
	BooleanType other = null;
	try {
	    other = (BooleanType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
	if (data == other.data) {
	    return 0;
	} else if (data) {
	    return 1;
	} else {
	    return -1;
	}
    }
}
