// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.math.BigInteger;

import org.joval.intf.windows.registry.IDwordValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.io.LittleEndian;

/**
 * Representation of a Windows registry DWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DwordValue extends Value implements IDwordValue {
    protected BigInteger data;

    public DwordValue(IKey parent, String name, BigInteger data) {
	type = Type.REG_DWORD;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public BigInteger getData() {
	return data;
    }

    public String toString() {
	return "DwordValue [Name=\"" + name + "\", Value=0x" + String.format("%08x", data.intValue()) + "]";
    }
}
