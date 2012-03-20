// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.ILoggable;

/**
 * An interface for accessing a Windows registry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRegistry extends ILoggable {
    String COMPUTERNAME_KEY = "System\\CurrentControlSet\\Control\\ComputerName\\ComputerName";
    String COMPUTERNAME_VAL = "ComputerName";

    String HKLM		= "HKEY_LOCAL_MACHINE";
    String HKU		= "HKEY_USERS";
    String HKCU		= "HKEY_CURRENT_USER";
    String HKCR		= "HKEY_CLASSES_ROOT";
    String DELIM_STR	= "\\";
    char   DELIM_CH	= '\\';

    /**
     * Get an environment based on this registry view.
     */
    public IEnvironment getEnvironment();

    /**
     * Get Windows license data from the registry.
     */
    public ILicenseData getLicenseData();

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
     * Return a key from a hive using the specified redirection mode.
     */
    public IKey fetchKey(String hive, String path) throws NoSuchElementException;

    /**
     * Return a subkey of a key using the specified redirection mode.
     */
    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException;

    /**
     * Return a value of a key, whose name matches a pattern.
     */
    public IValue[] fetchValues(IKey key, Pattern p) throws NoSuchElementException;

    /**
     * Return a value of a key, given its name.
     */
    public IValue fetchValue(IKey key, String name) throws NoSuchElementException;
}
