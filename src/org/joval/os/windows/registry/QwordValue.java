// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IQwordValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.io.LittleEndian;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Representation of a Windows registry QWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class QwordValue extends Value implements IQwordValue {
    private long data;

    /**
     * Conversion of a little-endian 8-byte QWORD to an int.
     */
    public static final long byteArrayToLong(byte[] b) throws IllegalArgumentException {
	if (b == null || b.length != 8) {
	    throw new IllegalArgumentException("buffer should be 8 bytes");
	}
	return LittleEndian.getULong(b);
    }

    public QwordValue(IKey parent, String name, long data) {
	type = REG_QWORD;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public long getData() {
	return data;
    }

    public String toString() {
	return "QwordValue [Name=\"" + name + "\", Value=0x" + String.format("%016x", data) + "]";
    }
}
