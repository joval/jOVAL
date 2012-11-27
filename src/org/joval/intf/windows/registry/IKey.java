// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Interface for a Windows registry key.  Can be used in conjunction with an IRegistry to browse child keys and values.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IKey {
    /**
     * Close any underlying resources associated with this key.
     */
    public boolean close();

    /**
     * Close any underlying resources associated with this key, and do the same for all of its <i>ancestor</i> keys.
     */
    public boolean closeAll();

    /**
     * Get this key's name (the last element of its path).
     */
    public String getName();

    /**
     * Get a string representation of this key.  Corresponds to getHive() + \\ + getPath().
     */
    public String toString();

    /**
     * Get the hive for this key.
     */
    public String getHive();

    /**
     * Get the path for this key underneath its hive.
     */
    public String getPath();

    /**
     * Indicates whether this key has a subkey with the given name.
     */
    public boolean hasSubkey(String name);

    /**
     * Returns an array of subkey names.
     */
    public String[] listSubkeys() throws Exception;

    /**
     * Returns an array of subkey names matching (filtered by) the given Pattern.
     */
    public String[] listSubkeys(Pattern p) throws Exception;

    /**
     * Get a value of this key.
     */
    public IValue getValue(String name) throws Exception;

    /**
     * Test if this key has a value with the given name.
     */
    public boolean hasValue(String name);

    /**
     * Returns an array of the names of this key's values.
     */
    public String[] listValues() throws Exception;

    /**
     * Returns an array of the names of this key's values matching (filtered by) the given Pattern.
     */
    public String[] listValues(Pattern p) throws Exception;
}
