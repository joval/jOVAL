// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.NoSuchElementException;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.os.windows.registry.RegistryException;

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

    /**
     * An enumeration of the registry hives.
     */
    enum Hive {
	HKCR("HKCR", "HKEY_CLASSES_ROOT", 0x80000000L),
	HKCU("HKCU", "HKEY_CURRENT_USER", 0x80000001L),
	HKLM("HKLM", "HKEY_LOCAL_MACHINE", 0x80000002L),
	HKU("HKU", "HKEY_USERS", 0x80000003L),
	HKCC("HKCC", "HKEY_CURRENT_CONFIG", 0x80000005L),
	HKDD(null, "HKEY_DYN_DATA", 0x80000006L);

	private String shortName, name;
	private long id;

	private Hive(String shortName, String name, long id) {
	    this.shortName = shortName;
	    this.name = name;
	    this.id = id;
	}

	public String getShortName() {
	    return shortName;
	}

	public String getName() {
	    return name;
	}

	public long getId() {
	    return id;
	}

	public static Hive fromName(String name) {
	    for (Hive hive : values()) {
		if (hive.getName().equals(name.toUpperCase())) {
		    return hive;
		}
	    }
	    return Hive.HKLM;
	}
    }

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
     * Get an ISearchable for the registry.
     */
    ISearchable<IKey> getSearcher();

    /**
     * Get a particular hive.
     */
    IKey getHive(Hive hive);

    /**
     * Return a key using its full path (including hive name).
     */
    IKey getKey(String fullPath) throws NoSuchElementException, RegistryException;

    /**
     * Return a key from a hive using the specified redirection mode.
     */
    IKey getKey(Hive hive, String path) throws NoSuchElementException, RegistryException;

    /**
     * Return the names of the subkeys of the specified key.
     */
    IKey[] enumSubkeys(IKey key) throws RegistryException;

    /**
     * Return a particular value of a key, given its name.
     *
     * @param name use null to retrieve the default value
     */
    IValue getValue(IKey key, String name) throws NoSuchElementException, RegistryException;

    /**
     * Return all the values of a key.
     */
    IValue[] enumValues(IKey key) throws RegistryException;
}
