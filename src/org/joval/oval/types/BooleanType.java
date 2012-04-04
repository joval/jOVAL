// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Binary type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BooleanType implements IType<BooleanType> {
    boolean data;

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

    public String toString() {
	return data ? "1" : "0";
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.BOOLEAN;
    }

    // Implement Comparable

    public int compareTo(BooleanType other) {
	if (data == other.data) {
	    return 0;
	} else if (data) {
	    return 1;
	} else {
	    return -1;
	}
    }
}
