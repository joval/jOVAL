// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.system.IEnvironment;

/**
 * An interface for accessing a Windows registry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRegistry {
    String HKLM		= "HKEY_LOCAL_MACHINE";
    String HKU		= "HKEY_USERS";
    String HKCU		= "HKEY_CURRENT_USER";
    String HKCR		= "HKEY_CLASSES_ROOT";
    String DELIM_STR	= "\\";
    char   DELIM_CH	= '\\';

    /**
     * Connect to the registry.
     */
    public boolean connect();

    /**
     * Disconnect from any underlying resources used to access this registry.
     */
    public void disconnect();

    /**
     * Set the 32-bit Windows on 64-bit Windows redirection behavior.
     */
    public void set64BitRedirect(boolean redirect);

    /**
     * Return whether or not this IRegistry is connected to a 64-bit view.
     */
    public boolean is64Bit();

    /**
     * Get an environment based on this registry view.
     */
    public IEnvironment getEnvironment();

    /**
     * Get a particular hive.  Accepts the HK constants.
     */
    public IKey getHive(String name) throws IllegalArgumentException;

    /**
     * Search for a key in the registry.
     */
    public List<IKey> search(String hive, String path) throws NoSuchElementException;

    /**
     * Return a key given its full path (including the name of the hive).
     */
    public IKey fetchKey(String fullPath) throws IllegalArgumentException, NoSuchElementException;

    /**
     * Return a key from a hive.
     */
    public IKey fetchKey(String hive, String path) throws NoSuchElementException;

    /**
     * Return a subkey of a key.
     */
    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException;

    /**
     * Return a value of a key, whose name matches a pattern.
     */
    public IValue fetchValue(IKey key, Pattern p);

    /**
     * Return a value of a key, given its name.
     */
    public IValue fetchValue(IKey key, String name) throws NoSuchElementException;
}
