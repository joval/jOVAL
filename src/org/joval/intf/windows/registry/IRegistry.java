// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.List;
import java.util.regex.Pattern;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;

/**
 * An interface for accessing a Windows registry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRegistry extends ILoggable {
    /**
     * Search condition field for the hive.
     */
    int FIELD_HIVE = 100;

    /**
     * Search condition field for the key path or pattern.
     */
    int FIELD_KEY = 101;

    /**
     * Search condition field for the value name or pattern.
     */
    int FIELD_VALUE = 102;

    String COMPUTERNAME_KEY	= "System\\CurrentControlSet\\Control\\ComputerName\\ComputerName";
    String COMPUTERNAME_VAL	= "ComputerName";
    String HKLM			= "HKEY_LOCAL_MACHINE";
    String HKU			= "HKEY_USERS";
    String HKCU			= "HKEY_CURRENT_USER";
    String HKCR			= "HKEY_CLASSES_ROOT";
    String DELIM_STR		= "\\";
    char   DELIM_CH		= '\\';
    String ESCAPED_DELIM	= "\\\\";

    /**
     * Get Windows license data from the registry.
     *
     * @throws Exception if there was a problem retrieving the license information.
     */
    ILicenseData getLicenseData() throws Exception;

    /**
     * Get a particular hive.  Accepts the HK constants.
     */
    IKey getHive(String name) throws IllegalArgumentException;

    /**
     * Get an ISearchable for the registry.
     */
    ISearchable<IKey> getSearcher();

    /**
     * Return a key given its full path (including the name of the hive).
     */
    IKey fetchKey(String fullPath) throws Exception;

    /**
     * Return a key from a hive using the specified redirection mode.
     */
    IKey fetchKey(String hive, String path) throws Exception;

    /**
     * Return a subkey of a key using the specified redirection mode.
     */
    IKey fetchSubkey(IKey parent, String name) throws Exception;

    /**
     * Return a value of a key, whose name matches a pattern.
     */
    IValue[] fetchValues(IKey key, Pattern p) throws Exception;

    /**
     * Return a value of a key, given its name.
     */
    IValue fetchValue(IKey key, String name) throws Exception;
}
