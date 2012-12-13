// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.math.BigInteger;

import org.joval.intf.windows.registry.IQwordValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.io.LittleEndian;

/**
 * Representation of a Windows registry QWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class QwordValue extends Value implements IQwordValue {
    private BigInteger data;

    public QwordValue(IKey parent, String name, BigInteger data) {
	type = Type.REG_QWORD;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public BigInteger getData() {
	return data;
    }

    public String toString() {
	return "QwordValue [Name=\"" + name + "\", Value=0x" + String.format("%016x", data.longValue()) + "]";
    }
}
