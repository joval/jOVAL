// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.io.LittleEndian;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Representation of a Windows registry DWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DwordValue extends Value implements IDwordValue {
    private int data;

    /**
     * Conversion of a little-endian 4-byte DWORD to an int.
     */
    public static final int byteArrayToInt(byte [] b) throws IllegalArgumentException {
	if (b == null || b.length != 4) {
	    throw new IllegalArgumentException("buffer should be 4 bytes");
	}
        return LittleEndian.getUInt(b);
    }

    public DwordValue(IKey parent, String name, int data) {
	type = REG_DWORD;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public int getData() {
	return data;
    }

    public String toString() {
	return "DwordValue [Name=\"" + name + "\", Value=0x" + String.format("%08x", data) + "]";
    }
}
