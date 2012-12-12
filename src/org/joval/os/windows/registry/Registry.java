// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.util.ISearchable;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.ILicenseData;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * A class for accessing the Windows registry using Powershell.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Registry implements IRegistry {
    protected IWindowsSession.View view;
    protected IWindowsSession session;
    protected IRunspace runspace;
    protected RegistrySearcher searcher;
    protected ILicenseData license = null;
    protected LocLogger logger;
    protected IKey hklm, hku, hkcu, hkcr, hkcc;
    protected Map<String, IKey> keyMap;
    protected Map<String, IValue[]> valueMap;

    /**
     * Create a new Registry, connected to the default view.
     */
    public Registry(IWindowsSession session) throws Exception {
	this(session, session.getNativeView());
    }

    /**
     * Create a new Registry, connected to the specified view.
     */
    public Registry(IWindowsSession session, IWindowsSession.View view) throws Exception {
	this.session = session;
	logger = session.getLogger();
	this.view = view;
	for (IRunspace runspace : session.getRunspacePool().enumerate()) {
	    if (runspace.getView() == view) {
		this.runspace = runspace;
		break;
	    }
	}
	if (runspace == null) {
	    runspace = session.getRunspacePool().spawn(view);
	}
	keyMap = new HashMap<String, IKey>();
	valueMap = new HashMap<String, IValue[]>();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IRegistry

    public ILicenseData getLicenseData() throws Exception {
	if (license == null) {
	    license = new LicenseData(this);
	}
	return license;
    }

    public ISearchable<IKey> getSearcher() {
	if (searcher == null) {
	    try {
		searcher = new RegistrySearcher(session, runspace);
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return searcher;
    }

    public IKey getHive(Hive hive) {
	switch(hive) {
	  case HKLM:
	    if (hklm == null) {
		hklm = new Key(this, Hive.HKLM, null);
	    }
	    return hklm;

	  case HKU:
	    if (hku == null) {
		hku = new Key(this, Hive.HKU, null);
	    }
	    return hku;

	  case HKCU:
	    if (hkcu == null) {
		hkcu = new Key(this, Hive.HKCU, null);
	    }
	    return hkcu;

	  case HKCR:
	    if (hkcr == null) {
		try {
		    runspace.invoke("New-PSDrive -PSProvider registry -Root HKEY_CLASSES_ROOT -Name HKCR");
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		hkcr = new Key(this, Hive.HKCR, null);
	    }
	    return hkcr;

	  case HKCC:
	    if (hkcc == null) {
		try {
		    runspace.invoke("New-PSDrive -PSProvider registry -Root HKEY_CURRENT_CONFIG -Name HKCC");
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		hkcc = new Key(this, Hive.HKCC, null);
	    }
	    return hkcc;

	  default:
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE, hive.getName()));
	}
    }

    public IKey getKey(String fullPath) throws NoSuchElementException, RegistryException {
	Hive hive = null;
	String path = null;
	int ptr = fullPath.indexOf(DELIM_STR);
	if (ptr == -1) {
	    hive = Hive.fromName(fullPath);
	} else {
	    hive = Hive.fromName(fullPath.substring(0, ptr));
	    path = fullPath.substring(ptr+1);
	}
	return getKey(hive, path);
    }

    public IKey getKey(Hive hive, String path) throws NoSuchElementException, RegistryException {
	try {
	    IKey key = new Key(this, hive, path);
	    if (keyMap.containsKey(key.toString())) {
		return keyMap.get(key.toString());
	    }

	    StringBuffer sb = new StringBuffer("Test-Path -LiteralPath ").append(getItemPath(key));
	    String data = runspace.invoke(sb.toString());
	    if ("True".equalsIgnoreCase(data)) {
		keyMap.put(key.toString(), key);
		return key;
	    } else {
		throw new NoSuchElementException(key.toString());
	    }
	} catch (NoSuchElementException e) {
	    throw e;
	} catch (Exception e) {
	    throw new RegistryException(e);
	}
    }

    public IKey[] enumSubkeys(IKey key) throws RegistryException {
	try {
	    StringBuffer sb = new StringBuffer("Get-Item -LiteralPath ").append(getItemPath(key));
	    sb.append(" | %{$_.GetSubKeyNames()}");
	    String data = runspace.invoke(sb.toString());
	    if (data == null) {
		return new IKey[0];
	    } else {
		String[] names = data.split("\r\n");
		IKey[] subkeys = new IKey[names.length];
		Hive hive = key.getHive();
		String path = key.getPath();
		for (int i=0; i < subkeys.length; i++) {
		    subkeys[i] = new Key(this, hive, path == null ? names[i] : path + DELIM_STR + names[i]);
		}
		return subkeys;
	    }
	} catch (Exception e) {
	    throw new RegistryException(e);
	}
    }

    public IValue getValue(IKey key, String name) throws NoSuchElementException, RegistryException {
	for (IValue value : enumValues(key)) {
	    if (value.getName().equalsIgnoreCase(name)) {
		return value;
	    }
	}
	throw new NoSuchElementException(name);
    }

    public IValue[] enumValues(IKey key) throws RegistryException {
	if (valueMap.containsKey(key.toString())) {
	    return valueMap.get(key.toString());
	}
	try {
	    StringBuffer sb = new StringBuffer("& reg query \"");
	    sb.append(key.getHive().getShortName());
	    String path = key.getPath();
	    if (path != null) {
		sb.append(IRegistry.DELIM_STR).append(path);
	    }
	    sb.append("\" /v *");

	    Map<String, Tuple> tuples = new HashMap<String, Tuple>();
	    String regData = runspace.invoke(sb.toString());
	    int startIndex = regData.indexOf("\r\n", regData.indexOf(key.toString())) + 2;
	    int ptr = -1;
	    String name = null;
	    for (String line : regData.substring(startIndex).split("\r\n")) {
		if (line.length() == 0) {
		    name = null;
		} else if (line.startsWith("    ") && (ptr = line.indexOf("    REG_")) != -1) {
		    name = line.substring(4, ptr);
		    int end = line.indexOf("    ", ptr+1);
		    IValue.Type type = IValue.Type.typeOf(line.substring(ptr, end).trim());
		    StringBuffer data = new StringBuffer(line.substring(end+4));
		    if (name.equals("(Default)")) {
			if (!"(value not set)".equals(data.toString())) {
			    name = "";
			    tuples.put("", new Tuple(type, data));
			}
		    } else {
			tuples.put(name, new Tuple(type, data));
		    }
		} else if (name != null) {
		    // Continuation of data from the previous value
		    Tuple tuple = tuples.get(name);
		    tuples.put(name, new Tuple(tuple.type, tuple.data.append(line)));
		}
	    }

	    ArrayList<IValue> values = new ArrayList<IValue>();
	    for (Map.Entry<String, Tuple> entry : tuples.entrySet()) {
		IValue value = null;
		name = entry.getKey();
		String data = entry.getValue().data.toString();
		switch(entry.getValue().type) {
		  case REG_NONE:
		    value = new NoneValue(key, name);
		    break;
		  case REG_SZ:
		    value = new StringValue(key, name, data);
		    break;
		  case REG_EXPAND_SZ:
		    value = new ExpandStringValue(key, name, data);
		    break;
		  case REG_MULTI_SZ:
		    value = new MultiStringValue(key, name, data.split(Matcher.quoteReplacement("\\0")));
		    break;
		  case REG_DWORD:
		    if (data.startsWith("0x")) {
			value = new DwordValue(key, name, Integer.parseInt(data.substring(2), 16));
		    } else {
			value = new DwordValue(key, name, Integer.parseInt(data));
		    }
		    break;
		  case REG_QWORD:
		    if (data.startsWith("0x")) {
			value = new QwordValue(key, name, Long.parseLong(data.substring(2), 16));
		    } else {
			value = new QwordValue(key, name, Long.parseLong(data));
		    }
		    break;
		  case REG_BINARY:
		    byte[] buff = new byte[data.length()/2];
		    for (int i=0; i < buff.length; i++) {
			int index = i*2;
			buff[i] = (byte)(Integer.parseInt(data.substring(index, index+2), 16) & 0xFF);
		    }
		    value = new BinaryValue(key, name, buff);
		    break;
		}
		logger.trace(JOVALMsg.STATUS_WINREG_VALINSTANCE, value.toString());
		values.add(value);
	    }
	    IValue[] result = values.toArray(new IValue[values.size()]);
	    valueMap.put(key.toString(), result);
	    return result;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new RegistryException(e);
	}
    }

    class Tuple {
	IValue.Type type;
	StringBuffer data;

	Tuple(IValue.Type type, StringBuffer data) {
	    this.type = type;
	    this.data = data;
	}
    }

    // Private

    /**
     * Returns the quoted String suitable for passing as a -LiteralPath to Test-Path or Get-Item.
     */
    private String getItemPath(IKey key) {
	StringBuffer sb = new StringBuffer("\"Registry::").append(key.getHive().getName());
	String path = key.getPath();
	if (path != null) {
	    sb.append(IRegistry.DELIM_STR).append(path);
	}
	sb.append("\"");
	return sb.toString();
    }
}
