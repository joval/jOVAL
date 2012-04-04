// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

import org.joval.io.LittleEndian;

/**
 * Binary type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BinaryType implements IType<BinaryType> {
    byte[] data;

    public BinaryType(String data) throws NumberFormatException {
	this.data = new byte[data.length()];
	for (int i=0; i < this.data.length; i++) {
	    this.data[i] = Byte.parseByte(Character.toString(data.charAt(i)), 16);
	}
    }

    public BinaryType(byte[] data) {
	this.data = data;
    }

    public byte[] getData() {
	return data;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < data.length; i++) {
	    sb.append(LittleEndian.toHexString(data[i]));
	}
	return sb.toString().toLowerCase();
    }

    // Implement IType

    public SimpleDatatypeEnumeration getType() {
	return SimpleDatatypeEnumeration.BINARY;
    }

    // Implement Comparable

    public int compareTo(BinaryType other) {
	if (data.length > other.data.length) {
	    return 1;
	} else if (data.length < other.data.length) {
	    return -1;
	} else {
	    for (int i=0; i < data.length; i++) {
		if (data[i] > other.data[i]) {
		    return 1;
		} else if (data[i] < other.data[i]) {
		    return -1;
		}
	    }
	    return 0;
	}
    }
}
