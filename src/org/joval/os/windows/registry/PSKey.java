// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.util.JOVALMsg;

/**
 * Representation of a Windows registry key fetched using Powershell.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PSKey implements IKey {
    private PSKey parent;
    private PSRegistry registry;
    private String hive, path, name;
    private String[] subkeyNames, valueNames;
    private Map<String, IValue> values;

    /**
     * Create a root-level key.
     */
    PSKey(PSRegistry registry, String name) {
	parent = null;
	this.registry = registry;
	this.name = name;
	hive = name;
    }

    /**
     * Retrieve a subkey of this key.
     */
    PSKey getSubkey(String name) throws NoSuchElementException {
	int ptr = name.indexOf(IRegistry.DELIM_STR);
	if (ptr == -1) {
	    if (hasSubkey(name)) {
		return new PSKey(this, name);
	    } else {
		throw new NoSuchElementException(toString() + IRegistry.DELIM_STR + name);
	    }
	} else {
	    String next = name.substring(0, ptr);
	    return ((PSKey)registry.fetchSubkey(this, next)).getSubkey(name.substring(ptr+1));
	}
    }

    @Override
    public String toString() {
	if (path == null) {
	    StringBuffer sb = new StringBuffer(getName());
	    PSKey key = this;
	    while ((key = key.parent) != null) {
		sb = new StringBuffer(key.getName()).append(IRegistry.DELIM_CH).append(sb);
	    }
	    path = sb.toString();
	}
	return path;
    }

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

    public String getName() {
	return name;
    }

    public String getHive() {
	return hive;
    }

    /**
     * Returns an external form of the Key path beneath the Hive.  If 64-bit redirection is active, this will return the
     * apparent location of the Key, potentially hiding its true location in the registry.
     */
    public String getPath() {
	String s = toString();
	int ptr = s.indexOf(IRegistry.DELIM_STR);
	if (ptr > 0) {
	    return s.substring(ptr+1);
	} else {
	    return null;
	}
    }

    /**
     * Test whether or not this Key has a child Key with the given name.
     */
    public boolean hasSubkey(String name) {
	try {
	    for (String subKey : listSubkeys()) {
		if (subKey.equalsIgnoreCase(name)) {
		    return true;
		}
	    }
	} catch (Exception e) {
	    registry.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return false;
    }

    /**
     * List all the names of the subkeys of this key.  This method doesn't leave open Key objects that need to be closed
     * later on.
     */
    public String[] listSubkeys() throws Exception {
	return listSubkeys(null);
    }

    /**
     * List all the names of the subkeys of this key that match the given pattern.  This method doesn't leave open Key
     * objects that need to be closed later on.
     */
    public String[] listSubkeys(Pattern p) throws Exception {
	if (subkeyNames == null) {
	    StringBuffer sb = getItemCommand();
	    sb.append(" | %{$_.GetSubKeyNames()}");
	    String data = registry.runspace.invoke(sb.toString());
	    if (data == null) {
		subkeyNames = new String[0];
	    } else {
		subkeyNames = data.split("\r\n");
	    }
	}
	if (p == null) {
	    return subkeyNames;
	} else {
	    ArrayList<String> list = new ArrayList<String>();
	    for (String subkeyName : subkeyNames) {
		if (p.matcher(subkeyName).find()) {
		    list.add(subkeyName);
		}
	    }
	    return list.toArray(new String[list.size()]);
	}
    }

    /**
     * Return a Value under this Key.
     */
    public IValue getValue(String name) throws Exception {
	if (values != null && values.containsKey(name)) {
	    return values.get(name);
	}

	if (hasValue(name)) {
	    if (values == null) {
		values = new HashMap<String, IValue>();
	    }
	    StringBuffer sb = new StringBuffer("reg query \"");
	    sb.append(shortHive(hive));
	    String path = getPath();
	    if (path != null) {
		sb.append(IRegistry.DELIM_STR).append(path);
	    }
	    sb.append("\" ");
	    if (name.length() == 0) {
		sb.append("/ve");
	    } else {
		sb.append("/v \"").append(name).append("\"");
	    }
	    String data = registry.runspace.invoke(sb.toString());
	    int ptr = 0;
	    if (name.length() == 0) {
		ptr = data.indexOf("(Default)");
	    } else {
		ptr = data.indexOf(name);
	    }
	    ptr = data.indexOf("    REG_", ptr);
	    int end = data.indexOf("    ", ptr+1);
	    String type = data.substring(ptr, end).trim();

	    ptr = end+4;
	    end = data.lastIndexOf("\r\n");
	    String s = data.substring(ptr, end);

	    IValue value = null;
	    if ("REG_NONE".equals(type)) {
		value = new NoneValue(this, name);
	    } else if ("REG_SZ".equals(type)) {
		value = new StringValue(this, name, s);
	    } else if ("REG_EXPAND_SZ".equals(type)) {
		value = new ExpandStringValue(this, name, s);
	    } else if ("REG_MULTI_SZ".equals(type)) {
		String[] sa = s.split(Matcher.quoteReplacement("\\0"));
		value = new MultiStringValue(this, name, sa);
	    } else if ("REG_DWORD".equals(type)) {
		if (s.startsWith("0x")) {
		    value = new DwordValue(this, name, Integer.parseInt(s.substring(2), 16));
		} else {
		    value = new DwordValue(this, name, Integer.parseInt(s));
		}
	    } else if ("REG_QWORD".equals(type)) {
		if (s.startsWith("0x")) {
		    value = new QwordValue(this, name, Long.parseLong(s.substring(2), 16));
		} else {
		    value = new QwordValue(this, name, Long.parseLong(s));
		}
	    } else if ("REG_BINARY".equals(type)) {
		String[] lines = s.split("\r\n");
		StringBuffer binData = new StringBuffer();
		for (String line : lines) {
		    binData.append(line.trim());
		}
		s = binData.toString();
		byte[] buff = new byte[s.length()/2];
		for (int i=0; i < buff.length; i++) {
		    int index = i*2;
		    buff[i] = (byte)(Integer.parseInt(s.substring(index, index+2), 16) & 0xFF);
		}
		value = new BinaryValue(this, name, buff);
	    } else {
		throw new RuntimeException("Bad value type: " + type);
	    }
	    values.put(name, value);
	    registry.getLogger().trace(JOVALMsg.STATUS_WINREG_VALINSTANCE, value.toString());
	    return value;
	} else {
	    throw new NoSuchElementException(name);
	}
    }

    /**
     * Test whether or not the Value with the specified name exists under this Key.
     */
    public boolean hasValue(String name) {
	try {
	    for (String valueName : listValues()) {
		if (valueName.equals(name)) {
		    return true;
		}
	    }
	} catch (Exception e) {
	}
	return false;
    }

    public String[] listValues() throws Exception {
	return listValues(null);
    }

    /**
     * List all the names of the values contained by this key.
     */
    public String[] listValues(Pattern p) throws Exception {
	if (valueNames == null) {
	    StringBuffer sb = getItemCommand();
	    sb.append(" | %{$_.GetValueNames()}");
	    String data = registry.runspace.invoke(sb.toString());
	    if (data == null) {
		valueNames = new String[0];
	    } else {
		valueNames = data.split("\r\n");
	    }
	}
	if (p == null) {
	    return valueNames;
	} else {
	    ArrayList<String> list = new ArrayList<String>();
	    for (String valueName : valueNames) {
		if (p.matcher(valueName).find()) {
		    list.add(valueName);
		}
	    }
	    return list.toArray(new String[list.size()]);
	}
    }

    // Private

    /**
     * Create a key.
     */
    private PSKey(PSKey parent, String name) {
	registry = parent.registry;
	hive = parent.hive;
	this.parent = parent;
	this.name = name;
	registry.keyMap.put(toString(), this);
    }

    private StringBuffer getItemCommand() {
	StringBuffer sb = new StringBuffer("Get-Item -literalPath \"Registry::").append(hive);
	String path = getPath();
	if (path != null) {
	    sb.append(IRegistry.DELIM_STR).append(path);
	}
	sb.append("\"");
	return sb;
    }

    private String shortHive(String hiveName) {
	if (IRegistry.HKLM.equals(hiveName)) {
	    return "HKLM";
	} else if (IRegistry.HKCU.equals(hiveName)) {
	    return "HKCU";
	} else if (IRegistry.HKCR.equals(hiveName)) {
	    return "HKCR";
	} else if (IRegistry.HKU.equals(hiveName)) {
	    return "HKU";
	} else if ("HKEY_CURRENT_CONFIG".equals(hiveName)) {
	    return "HKCC";
	}
	return null;
    }
}
