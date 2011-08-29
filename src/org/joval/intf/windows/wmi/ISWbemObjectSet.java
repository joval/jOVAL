// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.wmi;

import java.util.Iterator;

import org.joval.os.windows.wmi.WmiException;

/**
 * An SWbemObjectSet object is a collection of SWbemObject objects.
 * 
 * @param <T> A class that extends SWbemObject
 * 
 */
public interface ISWbemObjectSet extends Iterable <ISWbemObject> {
    /**
     * Iterate over the objects in the set.
     */
    public Iterator<ISWbemObject> iterator();

    /**
     * Get the number of objects in the set.
     */
    public int getSize();

    /**
     * Get a named item from the set.
     */
    public ISWbemObject getItem(String itemName) throws WmiException;
}
