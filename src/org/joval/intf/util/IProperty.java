// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

/**
 * An interface representing something that can have properties.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProperty extends Iterable<String> {
    /**
     * Returns the value associates with the key.  Returns null if there is no corresponding value defined for the key.
     */
    String getProperty(String key);

    /**
     * Returns the value of key as an int.  Returns 0 if there is no corresponding value defined for the key.
     */
    int getIntProperty(String key);

    /**
     * Returns the value of key as an long.  Returns 0 if there is no corresponding value defined for the key.
     */
    long getLongProperty(String key);

    /**
     * Returns the value of key as a boolean.  Returns false if there is no corresponding value defined for the key.
     */
    boolean getBooleanProperty(String key);

    /**
     * Set the value for the specified key.
     */
    void setProperty(String key, String value);
}
