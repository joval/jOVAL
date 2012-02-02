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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALMsg;
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
    public Registry(IPathRedirector redirector, ILoggable log) {
	super(redirector, log);
	if (Platform.isWindows()) {
	    try {
		loadingEnv = true;
		env = new Environment(this);
		license = new LicenseData(this);
		loadingEnv = false;
	    } catch (Exception e) {
		log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    // Implement IRegistry

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
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, name));
	}

	if (hive == null) {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE, name));
	}
	return hive;
    }

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

    public IKey fetchKey(String hive, String path) throws NoSuchElementException {
	return fetchSubkey(getHive(hive), path);
    }

    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException {
	String fullPath = new StringBuffer(parent.toString()).append(DELIM_CH).append(name).toString();
	IKey key = null;
	if (redirector != null) {
	    String alt = redirector.getRedirect(fullPath);
	    if (alt != null) {
		log.getLogger().trace(JOVALMsg.STATUS_WINREG_REDIRECT, fullPath, alt);
		return fetchKey(alt);
	    }
	}
	return ((Key)parent).getSubkey(name);
    }

    public IValue[] fetchValues(IKey key, Pattern p) throws NoSuchElementException {
	String[] sa = key.listValues(p);
	IValue[] values = new IValue[sa.length];
	for (int i=0; i < sa.length; i++) {
	    values[i] = fetchValue(key, sa[i]);
	}
	return values;
    }

    public IValue fetchValue(IKey key, String name) throws NoSuchElementException {
	return key.getValue(name);
    }

    // Internal

    Value createValue(Key key, String name, Object data) throws IllegalArgumentException {
	Value val = null;
	if (data instanceof String[]) {
	    val = new MultiStringValue(key, name, (String[])data);
	} else if (data instanceof byte[]) {
	    val = new BinaryValue(key, name, (byte[])data);
	} else if (data instanceof Long) {
	    val = new QwordValue(key, name, ((Long)data).longValue());
	} else if (data instanceof Integer) {
	    val = new DwordValue(key, name, ((Integer)data).intValue());
	} else if (data instanceof String) {
	    //
	    // Guess whether this might be an Expand type by counting the number of times the character '%' appears.
	    // If it's > 0 and divisible by 2, then we assume it's expandable.
	    //
	    String value = (String)data;
	    int len = value.length();
	    int appearances = 0;
	    for (int i=0; i < len; i++) {
		char ch = value.charAt(i);
		if (ch == '%') {
		    appearances++;
		}
	    }
	    if (appearances > 0 && (appearances % 2) == 0 && !loadingEnv) {
		val = new ExpandStringValue(key, name, value, env.expand(value));
	    } else {
		val = new StringValue(key, name, value);
	    }
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_TYPE, data.getClass().getName()));
	}
	log.getLogger().trace(JOVALMsg.STATUS_WINREG_VALINSTANCE, val.toString());
	return val;
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
