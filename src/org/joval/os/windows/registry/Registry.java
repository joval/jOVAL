// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.net.UnknownHostException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ca.beq.util.win32.registry.RegistryKey;
import ca.beq.util.win32.registry.RegistryValue;
import ca.beq.util.win32.registry.ValueType;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A class for accessing the Windows registry over the network.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Registry extends BaseRegistry {
    private boolean loadingEnv = false;
    private Key hklm, hku, hkcu, hkcr;

    private static boolean libLoaded = false;

    /**
     * Create a new Registry, connected to the specified host using the specified Credential.
     */
    public Registry() {
	super();
	ia64 = "64".equals(System.getProperty("sun.arch.data.model"));
    }

    // Implement IRegistry

    /**
     * Connect to the remote host.  This causes the environment information to be retrieved.
     */
    public boolean connect() {
	if (env == null) {
	    try {
		if (!libLoaded) {
		    System.loadLibrary("jRegistryKey");
		    libLoaded = true;
		}
		loadingEnv = true;
		env = new Environment(this);
		ia64 = env.getenv(IEnvironment.WINARCH).indexOf("64") != -1;
		loadingEnv = false;
		return true;
	    } catch (UnsatisfiedLinkError e) {
		return false;
	    }
	} else {
	    return true;
	}
    }

    /**
     * Closes the connection to the remote host.  This will cause any open keys to be closed.
     */
    public void disconnect() {
	searchMap.clear();
    }

    /**
     * A convenience method to retrieve a top-level hive using its String name.
     */
    public IKey getHive(String name) throws IllegalArgumentException {
	Key hive = null;
	if (HKLM.equals(name)) {
	    hive = getHKLM();
	} else if (HKU.equals(name)) {
	    hive = getHKU();
	} else if (HKCU.equals(name)) {
	    hive = getHKCU();
	} else if (HKCR.equals(name)) {
	    hive = getHKCR();
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_WINREG_HIVE_NAME", name));
	}

	if (hive == null) {
	    throw new RuntimeException(JOVALSystem.getMessage("ERROR_WINREG_HIVE", name));
	}
	return hive;
    }

    /**
     * Returns a key given a path that includes the hive.
     */
    public IKey fetchKey(String fullPath) throws IllegalArgumentException, NoSuchElementException {
	int ptr = fullPath.indexOf(DELIM_STR);
	if (ptr == -1) {
	    return getHive(fullPath);
	} else {
	    String hive = fullPath.substring(0, ptr);
	    String path = fullPath.substring(ptr + 1);
	    return fetchKey(hive, path);
	}
    }

    public IKey fetchKey(String fullPath, boolean win32) throws IllegalArgumentException, NoSuchElementException {
	return fetchKey(fullPath);
    }

    /**
     * Retrieve a Key, first by attempting to retrieve the key from the local cache, then from the deepest (nearest)
     * ancestor key available from the cache.
     */
    public IKey fetchKey(String hive, String path) throws NoSuchElementException {
	return fetchSubkey(getHive(hive), path, get64BitRedirect());
    }

    public IKey fetchKey(String hive, String path, boolean win32) throws NoSuchElementException {
	return fetchSubkey(getHive(hive), path, win32);
    }

    /**
     * Retrieve a subkey, first by attempting to retrieve the subkey from the local cache, then from the deepest (nearest)
     * ancestor key available from the cache.  This method redirects to the 64-bit portion of the registry if it fails
     * to find the key in the 32-bit portion.
     *
     * @throws NoSuchElementException if there is no subkey with the specified name.
     */
    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException {
	return fetchSubkey(parent, name, get64BitRedirect());
    }

    public IKey fetchSubkey(IKey parent, String name, boolean win32) throws NoSuchElementException {
	IKey key = null;
	if (win32) {
	    String alt = getRedirect(parent.toString() + DELIM_STR + name);
	    if (alt != null) {
		return fetchKey(alt);
	    }
	}
	return ((Key)parent).getSubkey(name);
    }

    /**
     * Return the value of the Value with a name matching the given Pattern.
     */
    public IValue[] fetchValues(IKey key, Pattern p) throws NoSuchElementException {
	String[] sa = key.listValues(p);
	IValue[] values = new IValue[sa.length];
	for (int i=0; i < sa.length; i++) {
	    values[i] = fetchValue(key, sa[i]);
	}
	return values;
    }

    /**
     * Retrieve a value (converted to a String if necessary) from the specified Key.
     *
     * @returns null if no such value exists.
     */
    public IValue fetchValue(IKey key, String name) throws NoSuchElementException {
	return key.getValue(name);
    }

    // Internal

    Value createValue(Key key, RegistryValue value) throws IllegalArgumentException {
	String name = value.getName();
	ValueType type = value.getType();

	if (type == ValueType.REG_MULTI_SZ) {
	    String[] sa = (String[])value.getData();
	    return new MultiStringValue(key, name, sa);
	} else if (type == ValueType.REG_BINARY) {
	    byte[] data = (byte[])value.getData();
	    return new BinaryValue(key, name, data);
	} else if (type == ValueType.REG_DWORD || type == ValueType.REG_DWORD_LITTLE_ENDIAN) {
	    return new DwordValue(key, name, ((Integer)value.getData()).intValue());
	} else if (type == ValueType.REG_EXPAND_SZ) {
	    String raw = value.getStringValue();
	    String expanded = null;
	    if (!loadingEnv) {
		expanded = env.expand(raw);
	    }
	    return new ExpandStringValue(key, name, raw, expanded);
	} else if (type == ValueType.REG_SZ) {
	    return new StringValue(key, name, value.getStringValue());
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_WINREG_TYPE", value.getType()));
	}
    }

    // Private 

    /**
     * Get the HKEY_LOCAL_MACHINE key.
     */
    private Key getHKLM() {
	if (hklm == null) {
	    hklm = new Key(this, HKLM);
	}
	return hklm;
    }

    /**
     * Get the HKEY_USERS key.
     */
    private Key getHKU() {
	if (hku == null) {
	    hku = new Key(this, HKU);
	}
	return hku;
    }

    /**
     * Get the HKEY_CURRENT_USER key.
     */
    private Key getHKCU() {
	if (hkcu == null) {
	    hkcu = new Key(this, HKCU);
	}
	return hkcu;
    }

    /**
     * Get the HKEY_CLASSES_ROOT key.
     */
    private Key getHKCR() {
	if (hkcr == null) {
	    hkcr = new Key(this, HKCR);
	}
	return hkcr;
    }
}
