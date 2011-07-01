// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

/**
 * Interface to a Windows registry DWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDwordValue extends IValue {
    /**
     * Get the data.
     */
    public int getData();
}
