// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import org.joval.intf.oval.IType;

import org.joval.util.StringTools;

/**
 * A utility class for handling EVR Strings.  See:
 * http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-common-schema.html#SimpleDatatypeEnumeration
 * http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#EntityStateEVRStringType
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class EvrStringType extends AbstractType {
    private String data;

    public EvrStringType(String data) {
	this.data = data;
    }

    public String getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.EVR_STRING;
    }

    public String getString() {
	return data;
    }

    // Implement Comparable

    /**
     * This is a more or less exact reimplementation of the algorithm used by librpm's rpmvercmp(char* a, char* b)
     * function, as dictated by the OVAL specification.  Based on rpmvercmp.c.
     */
    public int compareTo(IType t) {
	EvrStringType other = null;
	try {
	    other = (EvrStringType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}

	//
	// Easy string comparison to check for equivalence
	//
	if (data.equals(other.data)) {
	    return 0;
	}

	byte[] b1 = data.getBytes();
	byte[] b2 = other.data.getBytes();
	int i1 = 0, i2 = 0;
	boolean isNum = false;

	//
	// Loop through each version segment of the EVRs and compare them
	//
	while (i1 < b1.length && i2 < b2.length) {
	    while (i1 < b1.length && !isAlphanumeric(b1[i1])) i1++;
	    while (i2 < b2.length && !isAlphanumeric(b2[i2])) i2++;

	    //
	    // If we ran into the end of either, we're done.
	    //
	    if (i1 == b1.length || i2 == b2.length) break;

	    //
	    // Grab the first completely alphanumeric segment and compare them
	    //
	    int start1 = i1, start2 = i2;
	    if (isNumeric(b1[i1])) {
		while (i1 < b1.length && isNumeric(b1[i1])) i1++;
		while (i2 < b2.length && isNumeric(b2[i2])) i2++;
		isNum = true;
	    } else {
		while (i1 < b1.length && isAlpha(b1[i1])) i1++;
		while (i2 < b2.length && isAlpha(b2[i2])) i2++;
		isNum = false;
	    }

	    if (i1 == start1) return -1; // arbitrary; shouldn't happen

	    if (i2 == start2) return (isNum ? 1 : -1);

	    if (isNum) {
		int int1 = Integer.parseInt(new String(b1).substring(start1, i1));
		int int2 = Integer.parseInt(new String(b2).substring(start2, i2));

		if (int1 > int2) return 1;
		if (int2 > int1) return -1;
	    }

	    int rc = new String(b1).substring(start1, i1).compareTo(new String(b2).substring(start2, i2));
	    if (rc != 0) {
		return (rc < 1 ? -1 : 1);
	    }
	}

	// Take care of the case where all segments compare identically, but only the separator chars differed
	if (i1 == b1.length && i2 == b2.length) return 0;

	// Whichever version still has characters left over wins
	if (i1 < b1.length) return 1;
	return -1;
    }

    // Private 

    private boolean isAlphanumeric(byte b) {
	return isAlpha(b) || isNumeric(b);
    }

    private boolean isAlpha(byte b) {
	return StringTools.isLetter(b);
    }

    private boolean isNumeric(byte b) {
	return '0' <= b && b <= '9';
    }
}
