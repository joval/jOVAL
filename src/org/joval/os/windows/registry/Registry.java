// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.net.UnknownHostException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import org.joval.util.Base64;
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
	runspace.loadModule(getClass().getResourceAsStream("Registry.psm1"));
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
	IKey key = new Key(this, hive, path);
	if (keyMap.containsKey(key.toString())) {
	    return keyMap.get(key.toString());
	}
	getHive(hive); // idempotent hive initialization
	String result = null;
	try {
	    result = runspace.invoke("Test-Path -LiteralPath " + getItemPath(key));
	} catch (Exception e) {
	    throw new RegistryException(e);
	}
	if ("True".equalsIgnoreCase(result)) {
	    keyMap.put(key.toString(), key);
	    return key;
	} else {
	    throw new NoSuchElementException(key.toString());
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
	StringBuffer sb = new StringBuffer("Print-RegValues -Hive ");
	sb.append(key.getHive().getName());
	String path = key.getPath();
	if (path != null) {
	    sb.append(" -Key \"").append(path).append("\"");
	}
	ArrayList<IValue> values = new ArrayList<IValue>();
	String data = null;
	try {
	    data = runspace.invoke(sb.toString());
	} catch (Exception e) {
	    throw new RegistryException(e);
	}
	if (data != null) {
	    Iterator<String> iter = StringTools.toList(data.split("\r\n")).iterator();
	    while(true) {
		try {
		    IValue value = nextValue(key, iter);
		    if (value == null) {
			break;
		    } else {
			values.add(value);
		    }
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	IValue[] result = values.toArray(new IValue[values.size()]);
	valueMap.put(key.toString(), result);
	return result;
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

    static final String START = "{";
    static final String END = "}";

    /**
     * Generate the next value for the key from the input.
     */
    private IValue nextValue(IKey key, Iterator<String> input) throws Exception {
        boolean start = false;
        while(input.hasNext()) {
            String line = input.next();
            if (line.trim().equals(START)) {
                start = true;
                break;
            }
        }
        if (start) {
	    String name = null;
	    IValue.Type type = null;
	    String data = null;
	    List<String> multiData = null;
            while(input.hasNext()) {
                String line = input.next();
                if (line.equals(END)) {
                    break;
                } else if (line.startsWith("Kind: ")) {
                    type = IValue.Type.fromKind(line.substring(6));
                } else if (line.startsWith("Name: ")) {
		    name = line.substring(6);
                } else if (line.startsWith("Data: ")) {
		    if (type == IValue.Type.REG_MULTI_SZ) {
			if (multiData == null) {
			    multiData = new ArrayList<String>();
			}
			multiData.add(line.substring(6));
		    } else {
			data = line.substring(6);
		    }
                } else if (data != null) {
		    // continuation of data beyond a line-break
		    data = data + line;
		} else if (multiData != null) {
		    // continuation of last data entry beyond a line-break
		    multiData.add(new StringBuffer(multiData.remove(multiData.size() - 1)).append(line).toString());
		}
            }
	    if (type != null) {
		IValue value = null;
		switch(type) {
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
		    value = new MultiStringValue(key, name, multiData == null ? null : multiData.toArray(new String[0]));
		    break;
		  case REG_DWORD:
		    value = new DwordValue(key, name, Integer.parseInt(data, 16));
		    break;
		  case REG_QWORD:
		    value = new QwordValue(key, name, Long.parseLong(data, 16));
		    break;
		  case REG_BINARY:
		    value = new BinaryValue(key, name, Base64.decode(data));
		    break;
		}
		logger.trace(JOVALMsg.STATUS_WINREG_VALINSTANCE, value.toString());
		return value;
	    }
        }
        return null;
    }
}
