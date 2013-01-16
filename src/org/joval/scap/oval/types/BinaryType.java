// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import jsaf.io.LittleEndian;

import org.joval.intf.scap.oval.IType;
import org.joval.util.JOVALMsg;

/**
 * Binary type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BinaryType extends AbstractType {
    byte[] data;

    public BinaryType(String data) throws IllegalArgumentException {
	if (data.length() % 2 == 1) {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_BINARY_LENGTH, data, data.length()));
	}
	int len = data.length() / 2;
	this.data = new byte[len];
	for (int i=0; i < len; i++) {
	    int ptr = i*2;
	    this.data[i] = (byte)(Integer.parseInt(data.substring(ptr, ptr+2), 16) & 0xFF);
	}
    }

    public BinaryType(byte[] data) {
	this.data = data;
    }

    public byte[] getData() {
	return data;
    }

    // Implement IType

    public Type getType() {
	return Type.BINARY;
    }

    public String getString() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < data.length; i++) {
	    sb.append(LittleEndian.toHexString(data[i]));
	}
	return sb.toString().toLowerCase();
    }

    // Implement Comparable

    public int compareTo(IType t) {
	BinaryType other = null;
	try {
	    other = (BinaryType)t.cast(getType());
	} catch (TypeConversionException e) {
	    throw new IllegalArgumentException(e);
	}
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
