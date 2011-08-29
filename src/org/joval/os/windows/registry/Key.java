// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.beq.util.win32.registry.KeyIterator;
import ca.beq.util.win32.registry.RegistryKey;
import ca.beq.util.win32.registry.RegistryValue;
import ca.beq.util.win32.registry.RootKey;
import ca.beq.util.win32.registry.ValueIterator;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IValue;
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
    private RegistryKey self;
    private String path, name;

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
	return self.getRootKey().toString();
    }

    /**
     * Returns an external form of the Key path beneath the Hive.  If 64-bit redirection is active, this will return the
     * apparent location of the Key, potentially hiding its true location in the registry.
     */
    public String getPath() {
	String s = registry.getOriginal(toString());
	return s.substring(s.indexOf(Registry.DELIM_STR)+1);
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
	return self.hasSubkey(name);
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
	    return registry.createValue(this, self.getValue(name));
	}
	throw new NoSuchElementException(name);
    }

    /**
     * Test whether or not the Value with the specified name exists under this Key.
     */
    public boolean hasValue(String name) {
	return self.hasValue(name);
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
	    self = new RegistryKey(RootKey.HKEY_LOCAL_MACHINE);
	} else if (Registry.HKU.equals(name)) {
	    self = new RegistryKey(RootKey.HKEY_USERS);
	} else if (Registry.HKCU.equals(name)) {
	    self = new RegistryKey(RootKey.HKEY_CURRENT_USER);
	} else if (Registry.HKCR.equals(name)) {
	    self = new RegistryKey(RootKey.HKEY_CLASSES_ROOT);
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
     * Create a key.
     */
    private Key(Key parent, String name) {
	registry = parent.registry;
	this.parent = parent;
	this.name = name;
	String parentPath = parent.self.getPath();
	if (parentPath.length() > 0) {
	    parentPath += Registry.DELIM_STR;
	}
	self = new RegistryKey(parent.self.getRootKey(), parentPath + name);
    }

    private class InternalKeyIterator implements Iterator<IKey> {
	Iterator iter;

	InternalKeyIterator() {
	    iter = Key.this.self.subkeys();
	}

	public boolean hasNext() {
	    return iter.hasNext();
	}

	public IKey next() {
	    return new Key(Key.this, ((RegistryKey)iter.next()).getName());
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    private class InternalValueIterator implements Iterator<IValue> {
	Iterator iter;

	InternalValueIterator() {
	    iter = Key.this.self.values();
	}

	public boolean hasNext() {
	    return iter.hasNext();
	}

	public IValue next() {
	    return Key.this.registry.createValue(Key.this, (RegistryValue)iter.next());
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }
}
