// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.INoneValue;
import org.joval.intf.windows.registry.IKey;

/**
 * Representation of a Windows registry NONE value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NoneValue extends Value implements INoneValue {
    public NoneValue(IKey parent, String name) {
	type = Type.REG_NONE;
	this.parent = parent;
	this.name = name;
    }

    public String toString() {
	return "NoneValue [Name=\"" + name + "\"]";
    }
}
