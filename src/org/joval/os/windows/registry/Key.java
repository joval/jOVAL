// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;

/**
 * Implementation of a registry key.  All registry access is actually performed through the IRegistry class, so any
 * IRegistry implementation making use of this class must not rely on it to provide any information apart from its hive,
 * path and name.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Key implements IKey {
    private IRegistry registry;
    private IRegistry.Hive hive;
    private String path;
    private IValue[] values;

    protected Key (IRegistry registry, IRegistry.Hive hive, String path) {
	this.registry = registry;
	this.hive = hive;
	this.path = path;
    }

    // Implement IKey

    @Override
    public String toString() {
	StringBuffer sb = new StringBuffer(hive.getName());
	if (path != null) {
	    sb.append(IRegistry.DELIM_STR);
	    sb.append(path);
	}
	return sb.toString();
    }

    public IRegistry.Hive getHive() {
	return hive;
    }

    public String getPath() {
	return path;
    }

    public String getName() {
	if (path == null) {
	    return null;
	} else {
	    int ptr = path.lastIndexOf(IRegistry.DELIM_STR);
	    if (ptr == -1) {
		return path;
	    } else {
		return path.substring(ptr + 1);
	    }
	}
    }

    public boolean hasSubkey(String name) {
	try {
	    for (String subkey : listSubkeys()) {
		if (subkey.equalsIgnoreCase(name)) {
		    return true;
		}
	    }
	} catch (RegistryException e) {
	}
	return false;
    }

    public String[] listSubkeys() throws RegistryException {
	return listSubkeys(null);
    }

    public String[] listSubkeys(Pattern p) throws RegistryException {
	ArrayList<String> result = new ArrayList<String>();
	for (IKey key : registry.enumSubkeys(this)) {
	    String name = key.getName();
	    if (p == null || p.matcher(name).find()) {
		result.add(name);
	    }
	}
	return result.toArray(new String[result.size()]);
    }

    public IKey getSubkey(String name) throws NoSuchElementException, RegistryException {
	if (getPath() == null) {
	    return registry.getKey(hive, name);
	} else {
	    return registry.getKey(hive, getPath() + IRegistry.DELIM_STR + name);
	}
    }

    public boolean hasValue(String name) {
	try {
	    for (IValue value : listValues()) {
		if (value.getName().equalsIgnoreCase(name)) {
		    return true;
		}
	    }
	} catch (RegistryException e) {
	}
	return false;
    }

    public IValue[] listValues() throws RegistryException {
	return listValues(null);
    }

    public IValue[] listValues(Pattern p) throws RegistryException {
	if (values == null) {
	    values = registry.enumValues(this);
	}
	ArrayList<IValue> result = new ArrayList<IValue>();
	for (IValue value : values) {
	    if (p == null || p.matcher(value.getName()).find()) {
		result.add(value);
	    }
	}
	return result.toArray(new IValue[result.size()]);
    }

    public IValue getValue(String name) throws NoSuchElementException, RegistryException {
	return registry.getValue(this, name);
    }
}
