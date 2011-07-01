// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.math.BigDecimal;

import org.joval.util.JOVALSystem;

/**
 * A representation of a "version", which is a '.'-delimited series of integers.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Version {
    /**
     * Test whether str is of the form A.B.C.D
     */
    public static final boolean isVersion(String str) {
	try {
	    Version v = new Version(str);
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

    private int[] parts;

    public Version (Object object) throws IllegalArgumentException, NumberFormatException {
	if (object instanceof String) {
	    build((String)object);
	} else if (object instanceof BigDecimal) {
	    build(((BigDecimal)object).toPlainString());
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_VERSION_CLASS", object.getClass().getName()));
	}
    }

    /**
     * Construct a 4-part version from a pair of integers.
     */
    public Version (int major, int minor) {
	this((0xFFFF0000 & major) >> 16, 0x0000FFFF & major, (0xFFFF0000 & minor) >> 16, 0x0000FFFF & minor);
    }

    public Version (int major_hi, int major_lo, int minor_hi, int minor_lo) {
	parts = new int[4];
	parts[0] = major_hi & 0xFFFF;
	parts[1] = major_lo & 0xFFFF;
	parts[2] = minor_hi & 0xFFFF;
	parts[3] = minor_lo & 0xFFFF;
    }

    /**
     * Answers the question: is this object's value greater than v's value?
     */
    public boolean greaterThan(Version v) {
	int num = Math.min(parts.length, v.parts.length);
	for (int i=0; i < num; i++) {
	    if (parts[i] > v.parts[i]) {
		return true;
	    } else if (parts[i] < v.parts[i]) {
		return false;
	    }
	}
	if (parts.length > v.parts.length) {
	    for (int i=num; i < parts.length; i++) {
		if (parts[i] > 0) {
		    return true;
		}
	    }
	    return false;
	}
	return false;
    }

    public boolean lessThan(Version v) {
	if (equals(v) || greaterThan(v)) {
	    return false;
	} else {
	    return true;
	}
    }

    public boolean lessThanOrEquals(Version v) {
	return v.greaterThan(this);
    }

    public boolean greaterThanOrEquals(Version v) {
	return !v.greaterThan(this);
    }

    public boolean equals(Version v) {
	if (parts.length == v.parts.length) {
	    for (int i=0; i < parts.length; i++) {
		if (parts[i] != v.parts[i]) {
		    return false;
		}
	    }
	    return true;
	}
	return false;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < parts.length; i++) {
	    if (i > 0) {
		sb.append('.');
	    }
	    sb.append(Integer.toString(parts[i]));
	}
	return sb.toString();
    }

    // Private

    private void build(String str) {
	String[] sa = str.split("[\\.,]");
	parts = new int[sa.length];
	if (parts.length > 0) {
	    for (int i=0; i < parts.length; i++) {
		parts[i] = Integer.parseInt(sa[i]);
	    }
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_VERSION_STR", str));
	}
    }
}
