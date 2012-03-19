// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IBinaryValue;
import org.joval.util.Base64;

/**
 * Representation of a Windows registry binary value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BinaryValue extends Value implements IBinaryValue {
    private byte[] data;

    public BinaryValue(IKey parent, String name, byte[] data) {
	type = REG_BINARY;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public byte[] getData() {
	return data;
    }

    public String toString() {
	return "BinaryValue [Name=\"" + name + "\", Value={" + Base64.encodeBytes(data) + "}]";
    }
}
