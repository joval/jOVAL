// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IValue;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Representation of a Windows registry key.  This object can be used to browse child keys and values.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Key implements IKey {
    private Key parent;
    private Registry registry;
    private WinReg.HKEY rootKey;
    private String path, name;
    private Map<String, Object> values;

    // Implement IKey

    /**
     * Close this Key and all its ancestors.
     */
    public boolean closeAll() {
	return true;
    }

    /**
     * Returns true if the Key is closed without an error, or if the Key was already closed.
     */
    public boolean close() {
	return true;
    }

    public Key getParent() {
	return parent;
    }

    public String getName() {
	return name;
    }

    /**
     * Returns the full path of this Key.  This is an internal form, which does not mask the true underlying Key (for
     * instance, if 64-bit redirection is active).
     */
    public String toString() {
        if (path == null) {
            StringBuffer sb = new StringBuffer(getName());
            Key key = this;
            while ((key = key.getParent()) != null) {
                sb = new StringBuffer(key.getName()).append(Registry.DELIM_CH).append(sb);
            }
            path = sb.toString();
        }
        return path;
    }

    public String getHive() {
	if (rootKey == WinReg.HKEY_LOCAL_MACHINE) {
	    return Registry.HKLM;
	} else if (rootKey == WinReg.HKEY_USERS) {
	    return Registry.HKU;
	} else if (rootKey == WinReg.HKEY_CURRENT_USER) {
	    return Registry.HKCU;
	} else if (rootKey == WinReg.HKEY_CLASSES_ROOT) {
	    return Registry.HKCU;
	} else {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, rootKey));
	}
    }

    /**
     * Returns an external form of the Key path beneath the Hive.  If 64-bit redirection is active, this will return the
     * apparent location of the Key, potentially hiding its true location in the registry.
     */
    public String getPath() {
	String s = toString();
	int ptr = s.indexOf(Registry.DELIM_STR);
	if (ptr > 0) {
	    return s.substring(ptr+1);
	} else {
	    return null;
	}
    }

    public Iterator<IKey> subkeys() throws IllegalStateException {
	return new InternalKeyIterator();
    }

    public Iterator<IValue> values() throws IllegalStateException {
	return new InternalValueIterator();
    }

    /**
     * Test whether or not this Key has a child Key with the given name.
     */
    public boolean hasSubkey(String name) {
	StringBuffer sb = new StringBuffer(getPathInternal());
	if (sb.length() > 0) {
	    sb.append(Registry.DELIM_CH);
	}
	sb.append(name);

	try {
	    return Advapi32Util.registryKeyExists(rootKey, sb.toString());
	} catch (Win32Exception e) {
	    switch(e.getHR().intValue()) {
	      case 0x80070005: {
		String subkeyPath = getHive() + Registry.DELIM_STR + sb.toString();
		registry.getLogger().warn(JOVALMsg.ERROR_WINREG_ACCESS, subkeyPath);
		break;
	      }
	      default:
		registry.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		break;
	    }
	    return false;
	}
    }

    /**
     * List all the names of the subkeys of this key.  This method doesn't leave open Key objects that need to be closed
     * later on.
     */
    public String[] listSubkeys() throws IllegalStateException {
	return listSubkeys(null);
    }

    /**
     * List all the names of the subkeys of this key that match the given pattern.  This method doesn't leave open Key
     * objects that need to be closed later on.
     */
    public String[] listSubkeys(Pattern p) throws IllegalStateException {
	ArrayList<String> list = new ArrayList<String>();
	Iterator<IKey> iter = new InternalKeyIterator();
	while(iter.hasNext()) {
	    IKey subkey = iter.next();
	    String name = subkey.getName();
	    if (p == null) {
		list.add(name);
	    } else if (p.matcher(name).find()) {
		list.add(name);
	    }
	}
	return list.toArray(new String[list.size()]);
    }

    /**
     * Get the name of the nth subkey of this Key.
     */
    public String getSubkeyName(int n) throws NoSuchElementException, IllegalStateException {
	return listSubkeys()[n];
    }

    /**
     * Return a Value under this Key.
     */
    public IValue getValue(String name) throws NoSuchElementException, IllegalStateException {
	if (hasValue(name)) {
	    Iterator<IValue> iter = new InternalValueIterator();
	    while(iter.hasNext()) {
		IValue val = iter.next();
		if (val.getName().equals(name)) {
		    return val;
		}
	    }
	}
	throw new NoSuchElementException(name);
    }

    /**
     * Test whether or not the Value with the specified name exists under this Key.
     */
    public boolean hasValue(String name) {
	return Advapi32Util.registryValueExists(rootKey, getPathInternal(), name);
    }

    public String[] listValues() throws IllegalStateException {
	return listValues(null);
    }

    /**
     * List all the names of the values contained by this key.
     */
    public String[] listValues(Pattern p) throws IllegalStateException {
	ArrayList<String> list = new ArrayList<String>();
	Iterator<IValue> iter = new InternalValueIterator();
	while(iter.hasNext()) {
	    IValue value = iter.next();
	    String name = value.getName();
	    if (p == null) {
		list.add(name);
	    } else if (p.matcher(name).find()) {
		list.add(name);
	    }
	}
	return list.toArray(new String[list.size()]);
    }

    /**
     * Get the name of the nth value of this Key.
     */
    public String getValueName(int n) throws NoSuchElementException, IllegalStateException {
	return listValues()[n];
    }

    // Package-level access

    /**
     * Create a root-level key.
     */
    Key(Registry registry, String name) {
	parent = null;
	this.registry = registry;
	this.name = name;
	if (Registry.HKLM.equals(name)) {
	    rootKey = WinReg.HKEY_LOCAL_MACHINE;
	} else if (Registry.HKU.equals(name)) {
	    rootKey = WinReg.HKEY_USERS;
	} else if (Registry.HKCU.equals(name)) {
	    rootKey = WinReg.HKEY_CURRENT_USER;
	} else if (Registry.HKCR.equals(name)) {
	    rootKey = WinReg.HKEY_CLASSES_ROOT;
	} else {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, name));
	}
    }

    /**
     * Retrieve a subkey of this key.
     */
    Key getSubkey(String name) throws NoSuchElementException, IllegalStateException {
	if (hasSubkey(name)) {
	    return new Key(this, name);
	} else {
	    throw new NoSuchElementException(toString() + Registry.DELIM_STR + name);
	}
    }

    // Private

    /**
     * Get the non-redirected path.
     */
    String getPathInternal() {
	String s = toString();
	int ptr = s.indexOf(Registry.DELIM_STR);
	if (ptr > 0) {
	    return s.substring(ptr+1);
	} else {
	    return "";
	}
    }

    /**
     * Create a key.
     */
    private Key(Key parent, String name) {
	registry = parent.registry;
	rootKey = parent.rootKey;
	this.parent = parent;
	this.name = name;
    }

    private class InternalKeyIterator implements Iterator<IKey> {
	String[] subkeys;
	int index = 0;

	InternalKeyIterator() {
	    if (parent == null) {
		subkeys = Advapi32Util.registryGetKeys(rootKey);
	    } else {
		subkeys = Advapi32Util.registryGetKeys(rootKey, getPathInternal());
	    }
	}

	public boolean hasNext() {
	    return index < subkeys.length;
	}

	public IKey next() {
	    return new Key(Key.this, subkeys[index++]);
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    private class InternalValueIterator implements Iterator<IValue> {
	Iterator<String> iter;

	InternalValueIterator() {
	    if (values == null) {
		if (parent == null) {
		    values = Advapi32Util.registryGetValues(rootKey);
		} else {
		    values = Advapi32Util.registryGetValues(rootKey, getPathInternal());
		}
	    }
	    iter = values.keySet().iterator();
	}

	public boolean hasNext() {
	    return iter.hasNext();
	}

	public IValue next() {
	    String name = iter.next();
	    return Key.this.registry.createValue(Key.this, name, values.get(name));
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }
}
