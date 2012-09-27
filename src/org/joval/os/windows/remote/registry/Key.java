// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.registry;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;
import org.jinterop.winreg.IJIWinReg;
import org.jinterop.winreg.JIPolicyHandle;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.registry.RegistryException;
import org.joval.util.JOVALMsg;

/**
 * Representation of a Windows registry key.  This object can be used to browse child keys and values.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Key implements IKey {
    private Registry registry;
    private Key parent;
    private String name, path;

    JIPolicyHandle handle;
    boolean open = true;

    // Implement IKey

    /**
     * Close this Key and all its ancestors.
     */
    public boolean closeAll() {
	if (close()) {
	    if (parent == null) {
		return true; // Root!
	    } else {
		return parent.closeAll();
	    }
	} else {
	    return false;
	}
    }

    /**
     * Returns true if the Key is closed without an error, or if the Key was already closed.
     */
    public boolean close() {
	if (open) {
	    open = false;
	    try {
		registry.wrCloseKey(handle);
		registry.deregisterKey(this);
		registry.getLogger().trace(JOVALMsg.STATUS_WINREG_KEYCLOSED, toString());
		return true;
	    } catch (JIException e) {
		registry.getLogger().debug(JOVALMsg.ERROR_WINREG_KEYCLOSE, toString(), e.getMessage());
		registry.getLogger().trace(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		return false;
	    }
	}
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
	    StringBuffer sb = new StringBuffer(name);
	    Key key = this;
	    while ((key = key.getParent()) != null) {
		sb = new StringBuffer(key.getName()).append(Registry.DELIM_CH).append(sb);
	    }
	    path = sb.toString();
	}
	return path;
    }

    public String getHive() {
	if (parent == null) {
	    return toString();
	} else {
	    return parent.getHive();
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
	checkOpen();
	return new KeyIterator();
    }

    public Iterator<IValue> values() throws IllegalStateException {
	checkOpen();
	return new ValueIterator();
    }

    /**
     * Test whether or not this Key has a child Key with the given name.
     */
    public boolean hasSubkey(String name) {
	try {
	    IKey k = getSubkey(name);
	    k.close();
	    return true;
	} catch (NoSuchElementException e) {
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
	checkOpen();
	Vector <String>v = new Vector <String>();
	try {
	    int i=0;
	    while (true) {
		String name = getSubkeyName(i++);
		if (p == null) {
		    v.addElement(name);
		} else if (p.matcher(name).find()) {
		    v.addElement(name);
		}
	    }
	} catch (NoSuchElementException e) {
	    // no more subkeys
	}
	String[] s = new String[v.size()];
	v.copyInto(s);
	return s;
    }

    /**
     * Get the name of the nth subkey of this Key.
     */
    public String getSubkeyName(int n) throws NoSuchElementException, IllegalStateException {
	checkOpen();
	try {
	    String[] sa = registry.wrEnumKey(handle, n);
	    if (sa.length == 2) {
		return sa[0];
	    } else {
		registry.getLogger().warn(JOVALMsg.ERROR_WINREG_ENUMKEY, n, toString());
		return null;
	    }
	} catch (JIException e) {
	    switch(e.getErrorCode()) {
	      case 0x8001FFFF:
		if (recover()) {
		    return getSubkeyName(n);
		}

	      default:
		throw new RuntimeException(e);
	    }
	}
    }

    /**
     * Return a Value under this Key.
     */
    public IValue getValue(String name) throws NoSuchElementException, IllegalStateException {
	Iterator <IValue>i = values();
	while (i.hasNext()) {
	    IValue v = i.next();
	    if (v.getName().equalsIgnoreCase(name)) {
		return v;
	    }
	}
	throw new NoSuchElementException(name);
    }

    /**
     * Test whether or not the Value with the specified name exists under this Key.
     */
    public boolean hasValue(String name) {
	try {
	    getValue(name);
	    return true;
	} catch (NoSuchElementException e) {
	    return false;
	}
    }

    public String[] listValues() throws IllegalStateException {
	return listValues(null);
    }

    /**
     * List all the names of the values contained by this key.
     */
    public String[] listValues(Pattern p) throws IllegalStateException {
	checkOpen();
	Vector <String>v = new Vector <String>();
	try {
	    int i=0;
	    while (true) {
		String name = getValueName(i++);
		if (p == null) {
		    v.addElement(name);
		} else if (p.matcher(name).find()) {
		    v.addElement(name);
		}
	    }
	} catch (NoSuchElementException e) {
	    // no more subkeys
	}
	String[] s = new String[v.size()];
	v.copyInto(s);
	return s;
    }

    /**
     * Get the name of the nth value of this Key.
     */
    public String getValueName(int n) throws NoSuchElementException, IllegalStateException {
	checkOpen();
	try {
	    Object[] oa = registry.wrEnumValue(handle, n);
	    if (oa.length == 2) {
		return (String)oa[0];
	    } else {
		registry.getLogger().warn(JOVALMsg.ERROR_WINREG_ENUMVAL, n, toString());
		return null;
	    }
	} catch (JIException e) {
	    switch(e.getErrorCode()) {
	      case 0x8001FFFF:
		if (recover()) {
		    return getValueName(n);
		}

	      default:
		throw new RuntimeException(e);
	    }
	}
    }

    // Package-level access

    /**
     * Create a root-level key.
     */
    Key(Registry registry, String name, JIPolicyHandle handle) {
	parent = null;
	this.registry = registry;
	this.name = name;
	this.handle = handle;
	registry.registerKey(this);
    }

    /**
     * Retrieve a subkey of this key.
     */
    Key getSubkey(String name) throws NoSuchElementException, IllegalStateException {
	checkOpen();
	try {
	    return new Key(this, name, registry.wrOpenKey(handle, name, IJIWinReg.KEY_READ));
	} catch (JIException e) {
	    switch(e.getErrorCode()) {
	      case 0x8001FFFF:
		if (recover()) {
		    return getSubkey(name);
		}

	      default:
		throw new RuntimeException(e);
	    }
	}
    }

    // Private

    /**
     * Create a key.
     */
    private Key(Key parent, String name, JIPolicyHandle handle) {
	registry = parent.registry;
	this.parent = parent;
	this.name = name;
	this.handle = handle;
	registry.registerKey(this);
    }

    /**
     * Verify that the handle has not been closed.
     */
    private void checkOpen() throws IllegalStateException {
	if (!open) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.STATUS_WINREG_KEYCLOSED, toString()));
	}
    }

    /**
     * Recover from a connection that has inexplicably died.
     */
    private boolean recover() {
	synchronized(registry) {
	    registry.getLogger().warn(JOVALMsg.ERROR_WINREG_RECOVER);
	    registry.disconnect(false);
	    if (registry.connect()) {
		Key reincarnated = (Key)registry.fetchKey(getHive(), getPath());
		handle = reincarnated.handle;
		Key key = this;
		do {
		    key.parent = reincarnated.parent;
		    reincarnated = reincarnated.parent;
		} while((key = key.parent) != null);
		return true;
	    } else {
		return false;
	    }
	}
    }

    private class KeyIterator implements Iterator<IKey> {
	int index;
	IKey nextKey;

	KeyIterator() {
	    index = 0;
	    nextKey = null;
	}

	public boolean hasNext() {
	    try {
		nextKey = next();
	    } catch (NoSuchElementException e) {
		nextKey = null;
	    }
	    return nextKey != null;
	}

	public IKey next() {
	    if (nextKey != null) {
		IKey temp = nextKey;
		nextKey = null;
		return temp;
	    }

	    try {
		String[] sa = registry.wrEnumKey(handle, index++);
		if (sa.length == 2) {
		    String subkey = sa[0];
		    JIPolicyHandle hSubkey = registry.wrOpenKey(handle, subkey, IJIWinReg.KEY_READ);
		    return new Key(Key.this, subkey, hSubkey);
		} else {
		    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_ENUMKEY,
								   new Integer(index), Key.this.toString()));
		}
	    } catch (JIException e) {
		switch(e.getErrorCode()) {
		  case 0x8001FFFF:
		    if (recover()) {
			return next();
		    }

		  default:
		    throw new RuntimeException(e);
		}
	    }
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    private class ValueIterator implements Iterator<IValue> {
	int index;
	IValue nextValue;

	ValueIterator() {
	    index = 0;
	    nextValue = null;
	}

	public boolean hasNext() {
	    try {
		nextValue = next();
	    } catch (NoSuchElementException e) {
		nextValue = null;
	    }
	    return nextValue != null;
	}

	public IValue next() throws NoSuchElementException {
	    if (nextValue != null) {
		IValue temp = nextValue;
		nextValue = null;
		return temp;
	    }

	    try {
		Object[] oa = registry.wrEnumValue(handle, index++);
		if (oa.length == 2) {
		    String name = (String)oa[0];
		    return registry.createValue(Key.this, name);
		} else {
		    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_ENUMVAL,
								   Integer.toString(index), Key.this.toString()));
		}
	    } catch (JIException e) {
		switch(e.getErrorCode()) {
		  case 0x8001FFFF:
		    if (recover()) {
			return next();
		    }

		  default:
		    throw new RuntimeException(e);
		}
	    } catch (RegistryException e) {
		//
		// Just skip the bad value...
		//
		registry.getLogger().warn(e.getMessage());
		return next();
	    }
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }
}
