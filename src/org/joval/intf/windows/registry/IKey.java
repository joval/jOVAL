// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.os.windows.registry.RegistryException;

/**
 * Interface for a Windows registry key.  Can be used in conjunction with an IRegistry to browse child keys and values.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IKey {
    /**
     * Get a string representation of this key.  Corresponds to getHive() + \\ + getPath().
     */
    public String toString();

    /**
     * Get the hive for this key.
     */
    public IRegistry.Hive getHive();

    /**
     * Get the full path for this key underneath its hive.
     */
    public String getPath();

    /**
     * Get this key's name (the last element of its path).
     */
    public String getName();

    /**
     * Indicates whether this key has a subkey with the given name.
     */
    public boolean hasSubkey(String name);

    /**
     * Returns an array of subkey names.
     */
    public String[] listSubkeys() throws RegistryException;

    /**
     * Returns an array of subkey names matching (filtered by) the given Pattern.
     */
    public String[] listSubkeys(Pattern p) throws RegistryException;

    /**
     * Return a child of this key.
     */
    public IKey getSubkey(String name) throws NoSuchElementException, RegistryException;

    /**
     * Test if this key has a value with the given name.
     */
    public boolean hasValue(String name);

    /**
     * Returns an array of the names of this key's values.
     */
    public IValue[] listValues() throws RegistryException;

    /**
     * Returns an array of the names of this key's values matching (filtered by) the given Pattern.
     */
    public IValue[] listValues(Pattern p) throws RegistryException;

    /**
     * Get a value of this key.
     */
    public IValue getValue(String name) throws NoSuchElementException, RegistryException;
}
