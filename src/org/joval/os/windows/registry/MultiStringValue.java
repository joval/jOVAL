// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IMultiStringValue;

/**
 * Representation of a Windows registry multi-string value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class MultiStringValue extends Value implements IMultiStringValue {
    String[] data;

    public MultiStringValue(IKey parent, String name, String[] data) {
	type = REG_MULTI_SZ;
	this.parent = parent;
	this.name = name;
	this.data = data;
    }

    public String[] getData() {
	return data;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("MultiStringValue [Name=\"").append(name).append("\" Value={");
	for (int i=0; i < data.length; i++) {
	    if (i > 0) {
		sb.append(", ");
	    }
	    sb.append("\"").append(data[i]).append("\"");
	}
	sb.append("}]");
	return sb.toString();
    }
}
