// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IQwordValue;
import org.joval.intf.windows.registry.IKey;
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
     * Conversion of an int to a little-endian 8-byte QWORD.
     */
    public static final byte[] longToByteArray(long value) {
        return new byte[] {(byte)value,
			   (byte)(value >>> 8),
			   (byte)(value >>> 16),
			   (byte)(value >>> 24),
			   (byte)(value >>> 32),
			   (byte)(value >>> 40),
			   (byte)(value >>> 48),
			   (byte)(value >>> 56)};
    }

    /**
     * Conversion of a little-endian 8-byte QWORD to an int.
     */
    public static final int byteArrayToLong(byte [] b) throws IllegalArgumentException {
	if (b == null || b.length != 8) {
	    throw new IllegalArgumentException("buffer should be 8 bytes");
	}
        return	((b[7] & 0xFF) << 56) + ((b[6] & 0xFF) << 48) + ((b[5] & 0xFF) << 40) + ((b[4] & 0xFF) << 32) +
        	((b[3] & 0xFF) << 24) + ((b[2] & 0xFF) << 16) + ((b[1] & 0xFF) <<  8) +  (b[0] & 0xFF);
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
