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
public class PSRegistry implements IRegistry, ISearchable<IKey> {
    private IWindowsSession session;
    private IWindowsSession.View view;
    private ILicenseData license = null;
    private LocLogger logger;
    private Map<String, List<String>> searchMap;

    Map<String, IKey> keyMap;
    PSKey hklm, hku, hkcu, hkcr;
    IRunspace runspace;

    /**
     * Create a new Registry, connected to the default view.
     */
    public PSRegistry(IWindowsSession session) throws Exception {
	this(session, session.getNativeView());
    }

    /**
     * Create a new Registry, connected to the specified view.
     */
    public PSRegistry(IWindowsSession session, IWindowsSession.View view) throws Exception {
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
	searchMap = new HashMap<String, List<String>>();
	keyMap = new HashMap<String, IKey>();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement ISearchable<IKey>
 
    public ISearchable.ICondition condition(int field, int type, Object value) {
	return new GenericCondition(field, type, value);
    }

    public List<IKey> search(List<ISearchable.ICondition> conditions) throws Exception {
	String hive = null, keyPath = null, fullKeyPath = null, valName = null;
	Pattern keyPattern = null, valPattern = null;
	int maxDepth = DEPTH_UNLIMITED;
	boolean keyOnly = false;
	for (ISearchable.ICondition condition : conditions) {
	    switch(condition.getField()) {
	      case FIELD_DEPTH:
		maxDepth = ((Integer)condition.getValue()).intValue();
		break;
	      case FIELD_FROM:
		fullKeyPath = (String)condition.getValue();
		break;
	      case FIELD_HIVE:
		hive = (String)condition.getValue();
		break;
	      case FIELD_KEY:
		switch(condition.getType()) {
		  case TYPE_EQUALITY:
		    keyPath = (String)condition.getValue();
		    break;
		  case TYPE_PATTERN:
		    keyPattern = (Pattern)condition.getValue();
		    break;
		}
		break;
	      case FIELD_VALUE:
		switch(condition.getType()) {
		  case TYPE_EQUALITY:
		    valName = (String)condition.getValue();
		    break;
		  case TYPE_PATTERN:
		    valPattern = (Pattern)condition.getValue();
		    break;
		}
		break;
	    }
	}

	String[] keys = null;
	if (fullKeyPath == null) {
	    if (hive == null) {
		throw new IllegalArgumentException("Required search condition FIELD_HIVE is missing");
	    }
	    if (keyPath == null) {
		if (keyPattern != null) {
	 	    keys = guessParent(keyPattern, hive);
		}
		if (keys == null) {
		    keys = new String[]{null};
		}
	    } else {
		keys = new String[]{keyPath};
	    }
	} else {
	    IKey key = fetchKey(fullKeyPath);
	    hive = key.getHive();
	    keys = new String[]{key.getPath()};
	}

	List<IKey> results = new ArrayList<IKey>();
	for (String from : keys) {
	    StringBuffer sb = new StringBuffer("Find-RegKeys -Hive \"").append(hive).append("\"");
	    if (from != null) {
		sb.append(" -Key \"").append(from).append("\"");
	    }
	    if (keyPattern != null) {
		sb.append(" -Pattern \"").append(keyPattern.pattern()).append("\"");
	    }
	    if (valName != null) {
		sb.append(" -WithLiteralVal \"").append(valName).append("\"");
	    } else if (valPattern != null) {
		sb.append(" -WithValPattern \"").append(valPattern.pattern()).append("\"");
	    }
	    sb.append(" -Depth ").append(Integer.toString(maxDepth));
	    sb.append(" | %{$_.Name}");

	    String command = sb.toString();
	    if (searchMap.containsKey(command)) {
		for (String fullPath : searchMap.get(command)) {
		    results.add(fetchKey(fullPath));
		}
	    } else {
		String paths = runspace.invoke(sb.toString(), session.getTimeout(IWindowsSession.Timeout.XL));
		if (paths == null) {
		    searchMap.put(command, new ArrayList<String>());
		} else {
		    List<String> result = new ArrayList<String>();
		    searchMap.put(command, result);
		    for (String fullPath : paths.split("\r\n")) {
			result.add(fullPath);
			results.add(fetchKey(fullPath));
		    }
		}
	    }
	}
	return results;
    }

    /**
     * Return a list of Key paths containing potential matches for the specified pattern.
     */
    public String[] guessParent(Pattern p, Object... args) {
	String hive = null;
	for (Object arg : args) {
	    if (arg instanceof String) {
		hive = (String)arg;
		break;
	    }
	}

	String path = p.pattern();
	if (!path.startsWith("^")) {
	    return null;
	}
	path = path.substring(1);

	int ptr = path.indexOf(ESCAPED_DELIM);
	if (ptr == -1) {
	    return Arrays.asList(path).toArray(new String[1]);
	}

	StringBuffer sb = new StringBuffer(path.substring(0,ptr));
	ptr += ESCAPED_DELIM.length();
	int next = ptr;
	while((next = path.indexOf(ESCAPED_DELIM, ptr)) != -1) {
	    String token = path.substring(ptr, next);
	    if (StringTools.containsRegex(token)) {
		break;
	    } else {
		sb.append(DELIM_STR).append(token);
		ptr = next + ESCAPED_DELIM.length();
	    }
	}
	if (sb.length() == 0) {
	    return null;
	} else {
	    String parent = sb.toString();

	    // One of the children of parent should match...
	    StringBuffer prefix = new StringBuffer("^");
	    String token = path.substring(ptr);
	    for (int i=0; i < token.length(); i++) {
		char c = token.charAt(i);
		boolean isRegexChar = false;
		for (char ch : StringTools.REGEX_CHARS) {
		    if (c == ch) {
			isRegexChar = true;
			break;
		    }
		}
		if (isRegexChar) {
		    break;
		} else {
		    prefix.append(c);
		}
	    }
	    try {
		if (prefix.length() > 1) {
		    IKey base = fetchKey(hive, parent);
		    ArrayList<String> paths = new ArrayList<String>();
		    for (String subkeyName : base.listSubkeys(Pattern.compile(prefix.toString()))) {
			paths.add(base.getPath() + IRegistry.DELIM_STR + subkeyName);
		    }
		    return paths.toArray(new String[paths.size()]);
		}
	    } catch (Exception e) {
	    }

	    return Arrays.asList(parent).toArray(new String[1]);
	}
    }

    // Implement IRegistry

    public ILicenseData getLicenseData() throws Exception {
	if (license == null) {
	    license = new LicenseData(this);
	}
	return license;
    }

    public ISearchable<IKey> getSearcher() {
	return this;
    }

    public IKey getHive(String name) throws IllegalArgumentException {
	PSKey hive = null;
	if (HKLM.equals(name)) {
	    hive = getHKLM();
	} else if (HKU.equals(name)) {
	    hive = getHKU();
	} else if (HKCU.equals(name)) {
	    hive = getHKCU();
	} else if (HKCR.equals(name)) {
	    hive = getHKCR();
	} else {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, name));
	}

	if (hive == null) {
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE, name));
	}
	return hive;
    }

    public IKey fetchKey(String fullPath) throws Exception {
	int ptr = fullPath.indexOf(DELIM_STR);
	if (ptr == -1) {
	    return getHive(fullPath);
	} else {
	    String hive = fullPath.substring(0, ptr);
	    String path = fullPath.substring(ptr + 1);
	    return fetchKey(hive, path);
	}
    }

    public IKey fetchKey(String hive, String path) throws Exception {
	return fetchSubkey(getHive(hive), path);
    }

    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException {
	String fullPath = new StringBuffer(parent.toString()).append(DELIM_CH).append(name).toString();
	if (keyMap.containsKey(fullPath)) {
	    return keyMap.get(fullPath);
	} else {
	    IKey key = ((PSKey)parent).getSubkey(name);
	    keyMap.put(fullPath, key);
	    return key;
	}
    }

    public IValue[] fetchValues(IKey key, Pattern p) throws Exception {
	String[] sa = key.listValues(p);
	IValue[] values = new IValue[sa.length];
	for (int i=0; i < sa.length; i++) {
	    values[i] = fetchValue(key, sa[i]);
	}
	return values;
    }

    public IValue fetchValue(IKey key, String name) throws Exception {
	return key.getValue(name);
    }

    // Private 

    /**
     * Get the HKEY_LOCAL_MACHINE key.
     */
    private PSKey getHKLM() {
	if (hklm == null) {
	    hklm = new PSKey(this, HKLM);
	}
	return hklm;
    }

    /**
     * Get the HKEY_USERS key.
     */
    private PSKey getHKU() {
	if (hku == null) {
	    hku = new PSKey(this, HKU);
	}
	return hku;
    }

    /**
     * Get the HKEY_CURRENT_USER key.
     */
    private PSKey getHKCU() {
	if (hkcu == null) {
	    hkcu = new PSKey(this, HKCU);
	}
	return hkcu;
    }

    /**
     * Get the HKEY_CLASSES_ROOT key.
     */
    private PSKey getHKCR() {
	if (hkcr == null) {
	    try {
		runspace.invoke("New-PSDrive -PSProvider registry -Root HKEY_CLASSES_ROOT -Name HKCR");
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    hkcr = new PSKey(this, HKCR);
	}
	return hkcr;
    }
}
