// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IValue;

/**
 * Abstract base class representing a Windows registry value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class Value implements IValue {
    protected Type type = Type.REG_NONE;
    protected String name = null;
    protected IKey parent = null;

    // Implement IValue

    public final Type getType() {
	return type;
    }

    public final IKey getKey() {
	return parent;
    }

    public final String getName() {
	return name;
    }

    public abstract String toString();
}
