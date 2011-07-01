// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

/**
 * Interface to an abstract Windows registry value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IValue {
    int REG_NONE	= 0;
    int REG_DWORD	= 1;
    int REG_BINARY	= 2;
    int REG_SZ		= 3;
    int REG_EXPAND_SZ	= 4;
    int REG_MULTI_SZ	= 5;

    /**
     * Returns the corresponding REG_ constant.
     */
    public int getType();

    /**
     * Return the Key under which this Value lies.
     */
    public IKey getKey();

    /**
     * Return the Value's name.
     */
    public String getName();

    /**
     * Returns a String suitable for logging about the Value.
     */
    public String toString();
}
