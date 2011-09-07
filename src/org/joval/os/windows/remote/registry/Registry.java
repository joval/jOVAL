// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.registry;

import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.winreg.IJIWinReg;
import org.jinterop.winreg.JIWinRegFactory;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.WindowsSystemInfo;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.os.windows.registry.BaseRegistry;
import org.joval.os.windows.registry.BinaryValue;
import org.joval.os.windows.registry.DwordValue;
import org.joval.os.windows.registry.ExpandStringValue;
import org.joval.os.windows.registry.MultiStringValue;
import org.joval.os.windows.registry.StringValue;
import org.joval.os.windows.registry.Value;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A class for accessing the Windows registry over the network.  Leverages the j-Interop package.
 *
 * One nice feature about this utility class is that it manages all the open references to registry keys
 * and will clean them up for you when you call the disconnect method.
 *
 * DAS: Need to complete the implementation of Wow64 registry redirection; see:
 *      http://msdn.microsoft.com/en-us/library/aa384253%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Registry extends BaseRegistry {
    public final static int DEFAULT_BUFFER_SIZE	= 2048;
    public final static int PATH_BUFFER_SIZE	= 2048;

    public final static String PATH		= "path";
    public final static String DEFAULT_ENCODING	= "US-ASCII";

    private final static JIWinRegFactory factory = JIWinRegFactory.getSingleTon();

    private String host;
    private WindowsCredential cred;
    private IJIWinReg registry;
    private Key hklm, hku, hkcu, hkcr;
    private Hashtable <String, Key> map;

    private static final int STATE_DISCONNECTED	= 1;
    private static final int STATE_ENV		= 2; // signifies the environment is being created
    private static final int STATE_CONNECTED	= 3;

    private int state = STATE_DISCONNECTED;

    /**
     * Create a new Registry, connected to the specified host using the specified WindowsCredential.
     */
    public Registry(String host, WindowsCredential cred) {
	super();
	this.host = host;
	this.cred = cred;
	map = new Hashtable <String, Key>();
    }

    // Implement IRegistry

    /**
     * Connect to the remote host.  This causes the environment information to be retrieved.
     */
    public boolean connect() {
	try {
	    JOVALSystem.getLogger().log(Level.FINER, "Connecting to registry: " + host);
	    registry = factory.getWinreg(cred, host, true);
	    state = STATE_ENV;
	    env = new Environment(this);
	    ia64 = env.getenv(IEnvironment.WINARCH).indexOf("64") != -1;
	    state = STATE_CONNECTED;
	    return true;
	} catch (UnknownHostException e) {
	    JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_UNKNOWN_HOST", host));
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	}
	return false;
    }

    /**
     * Closes the connection to the remote host.  This will cause any open keys to be closed.
     */
    public void disconnect() {
	try {
	    Enumeration <Key>keys = map.elements();
	    while (keys.hasMoreElements()) {
		Key key = keys.nextElement();
		if (key.open) {
		    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_WINREG_KEYCLEAN", key.toString()));
		    key.close();
		}
	    }
	    registry.closeConnection();
	    state = STATE_DISCONNECTED;
	    searchMap.clear();
	    map.clear();
	} catch (JIException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("STATUS_WINREG_DISCONNECT"), e);
	}
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
	return fetchKey(fullPath, redirector.isEnabled());
    }

    public IKey fetchKey(String fullPath, boolean win32) throws IllegalArgumentException, NoSuchElementException {
	int ptr = fullPath.indexOf(DELIM_STR);
	if (ptr == -1) {
	    return getHive(fullPath);
	} else {
	    String hive = fullPath.substring(0, ptr);
	    String path = fullPath.substring(ptr + 1);
	    return fetchKey(hive, path, win32);
	}
    }

    /**
     * Retrieve a Key, first by attempting to retrieve the key from the local cache, then from the deepest (nearest)
     * ancestor key available from the cache.
     */
    public IKey fetchKey(String hive, String path) throws NoSuchElementException {
	return fetchSubkey(getHive(hive), path);
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
	return fetchSubkey(parent, name, redirector.isEnabled());
    }

    public IKey fetchSubkey(IKey parent, String name, boolean win32) throws NoSuchElementException {
	String fullPath = new StringBuffer(parent.toString()).append(DELIM_CH).append(name).toString();
	Key key = map.get(fullPath);
	if (key == null) {
	    if (win32) {
		String alt = redirector.getRedirect(fullPath);
		if (alt != null) {
		    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_WINREG_REDIRECT", fullPath, alt));
		    return fetchKey(alt, false);
		}
	    }
	    StringBuffer partialPath = new StringBuffer();
	    StringTokenizer tok = new StringTokenizer(fullPath, DELIM_STR);
	    int numTokens = tok.countTokens();
	    for (int i=0; i < numTokens; i++) {
		if (i > 0) {
		    partialPath.append(DELIM_CH);
		}
		String next = tok.nextToken();
		partialPath.append(next);
		Key cached = map.get(partialPath.toString());
		if (cached == null) {
		    key = key.getSubkey(next);
		} else {
		    key = cached;
		}
	    }
	}
	return key;
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

    // Package-level access

    String unRedirect(String s) {
	return redirector.getOriginal(s);
    }

    IJIWinReg getWinreg() {
	return registry;
    }

    Value createValue(Key key, String name) throws IllegalArgumentException, JIException {
	int len = name.equalsIgnoreCase(PATH) ? PATH_BUFFER_SIZE : DEFAULT_BUFFER_SIZE;
	Object[] oa = registry.winreg_QueryValue(key.handle, name, len);
	if (oa.length == 2) {
	    int type = ((Integer)oa[0]).intValue();
	    switch(type) {
	      case IJIWinReg.REG_MULTI_SZ: {
		byte[][] data = (byte[][])oa[1];
		String[] sa = new String[data.length];
		for (int i=0; i < data.length; i++) {
		    sa[i] = getString(data[i]);
		}
		return new MultiStringValue(key, name, sa);
	      }

	      case IJIWinReg.REG_BINARY: {
		byte[] data = (byte[])oa[1];
		return new BinaryValue(key, name, data);
	      }

	      case IJIWinReg.REG_DWORD: {
        	return new DwordValue(key, name, DwordValue.byteArrayToInt((byte[])oa[1]));
	      }

	      case IJIWinReg.REG_EXPAND_SZ: {
		String raw = getString((byte[])oa[1]);
		String expanded = null;
		if (state != STATE_ENV) {
		    expanded = env.expand(raw);
		}
		return new ExpandStringValue(key, name, raw, expanded);
	      }

	      case IJIWinReg.REG_SZ:
		return new StringValue(key, name, getString((byte[])oa[1]));

	      case IJIWinReg.REG_NONE:
	      default:
		throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_WINREG_TYPE", type));
	    }
	}
	throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_WINREG_QUERYVAL", new Integer(oa.length)));
    }

    /**
     * All Keys are maintained in a Map with this Registry object so that they can be verified closed when disconnecting.
     * If a Key that is mapped is ever re-fetched, the original Key is closed and replaced in the map with the new copy.
     */
    void registerKey(Key key) {
	String path = key.toString();
	if (map.containsKey(path)) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINREG_REKEY", path));
	    Key k = map.get(path);
	    k.close();
	    map.remove(path);
	}
	map.put(path, key);
	JOVALSystem.getLogger().log(Level.FINEST, JOVALSystem.getMessage("STATUS_WINREG_KEYREG", path));
    }

    void deregisterKey(Key key) {
	String path = key.toString();
	if (HKLM.equals(path)) {
	    hklm = null;
	} else if (HKU.equals(path)) {
	    hku = null;
	} else if (HKCU.equals(path)) {
	    hkcu = null;
	} else if (HKCR.equals(path)) {
	    hkcr = null;
	}
	if (map.containsKey(path)) {
	    map.remove(path);
	    JOVALSystem.getLogger().log(Level.FINEST, JOVALSystem.getMessage("STATUS_WINREG_KEYDEREG", path));
	}
    }

    /**
     * Convert bytes to a String using the default encoding (US-ASCII).
     *
     * TBD: i18n
     */
    static final String getString(byte[] b) {
	String s = null;
	try {
	    if (b != null) {
		s = new String(b, Charset.forName(DEFAULT_ENCODING));
	    }
	} catch (IllegalCharsetNameException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINREG_CONVERSION"), e);
	} catch (UnsupportedCharsetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINREG_CONVERSION"), e);
	}
	return s;
    }

    // Private 

    /**
     * Get the HKEY_LOCAL_MACHINE key.
     */
    private Key getHKLM() {
	if (hklm == null) {
	    try {
		hklm = new Key(this, HKLM, registry.winreg_OpenHKLM());
	    } catch (JIException e) {
		JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_WINREG_HIVE_OPEN",  HKLM), e);
	    }
	}
	return hklm;
    }

    /**
     * Get the HKEY_USERS key.
     */
    private Key getHKU() {
	if (hku == null) {
	    try {
		hku = new Key(this, HKU, registry.winreg_OpenHKU());
	    } catch (JIException e) {
		JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_WINREG_HIVE_OPEN",  HKU), e);
	    }
	}
	return hku;
    }

    /**
     * Get the HKEY_CURRENT_USER key.
     */
    private Key getHKCU() {
	if (hkcu == null) {
	    try {
		hkcu = new Key(this, HKCU, registry.winreg_OpenHKCU());
	    } catch (JIException e) {
		JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_WINREG_HIVE_OPEN",  HKCU), e);
	    }
	}
	return hkcu;
    }

    /**
     * Get the HKEY_CLASSES_ROOT key.
     */
    private Key getHKCR() {
	if (hkcr == null) {
	    try {
		hkcr = new Key(this, HKCR, registry.winreg_OpenHKCR());
	    } catch (JIException e) {
		JOVALSystem.getLogger().log(Level.SEVERE, JOVALSystem.getMessage("ERROR_WINREG_HIVE_OPEN",  HKCR), e);
	    }
	}
	return hkcr;
    }
}
