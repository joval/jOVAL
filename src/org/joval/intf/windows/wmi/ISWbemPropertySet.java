// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.wmi;

import java.util.Iterator;

import org.joval.windows.wmi.WmiException;

/**
 * An ISWbemPropertySet is a collection of ISWbemProperties.
 */
public interface ISWbemPropertySet extends Iterable <ISWbemProperty> {
    /**
     * Iterate over the properties in the set.
     */
    public Iterator<ISWbemProperty> iterator();

    /**
     * Get the number of properties in the set.
     */
    public int getSize();

    /**
     * Get a property by its name.
     */
    public ISWbemProperty getItem(String itemName) throws WmiException;
}
