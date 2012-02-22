// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.registry;

import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIErrorCodes;
import org.jinterop.dcom.common.JIException;
import org.jinterop.winreg.IJIWinReg;
import org.jinterop.winreg.JIPolicyHandle;
import org.jinterop.winreg.JIWinRegFactory;

import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.os.windows.WindowsSystemInfo;
import org.joval.os.windows.registry.BaseRegistry;
import org.joval.os.windows.registry.BinaryValue;
import org.joval.os.windows.registry.DwordValue;
import org.joval.os.windows.registry.ExpandStringValue;
import org.joval.os.windows.registry.LicenseData;
import org.joval.os.windows.registry.MultiStringValue;
import org.joval.os.windows.registry.QwordValue;
import org.joval.os.windows.registry.StringValue;
import org.joval.os.windows.registry.Value;
import org.joval.os.windows.system.Environment;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A class for accessing the Windows registry over the network.  Leverages the j-Interop package.
 *
 * One nice feature about this utility class is that it manages all the open references to registry keys
 * and will clean them up for you when you call the disconnect method.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Registry extends BaseRegistry {
    public static final int BUFFER_LEN = 512;

    public static final String PATH		= "path";
    public static final String DEFAULT_ENCODING	= "US-ASCII";

    private static final long INTERVAL = 5000L; // a registry operation should never take longer than this
    private static final JIWinRegFactory factory = JIWinRegFactory.getSingleTon();

    private String host;
    private IWindowsCredential cred;
    private IJIWinReg winreg;
    private RegistryTask heartbeat;
    private Key hklm, hku, hkcu, hkcr;
    private Hashtable <String, Key> map;

    private static final int STATE_DISCONNECTED	= 1;
    private static final int STATE_ENV		= 2; // signifies the environment is being created
    private static final int STATE_CONNECTED	= 3;

    private int state = STATE_DISCONNECTED;

    /**
     * Create a new Registry, connected to the specified host using the specified IWindowsCredential.
     */
    public Registry(String host, IWindowsCredential cred, IPathRedirector redirector, ILoggable log) {
	super(redirector, log);
	this.host = host;
	this.cred = cred;
	map = new Hashtable <String, Key>();
	heartbeat = new RegistryTask();
    }

    /**
     * Connect to the remote host.  This causes the environment information to be retrieved.
     */
    public synchronized boolean connect() {
	try {
	    JOVALSystem.getTimer().scheduleAtFixedRate(heartbeat, INTERVAL, INTERVAL);
	    log.getLogger().trace(JOVALMsg.STATUS_WINREG_CONNECT, host);
	    winreg = factory.getWinreg(new AuthInfo(cred), host, true);
	    state = STATE_ENV;
	    env = new Environment(this);
	    license = new LicenseData(this);
	    state = STATE_CONNECTED;
	    return true;
	} catch (UnknownHostException e) {
	    log.getLogger().error(JOVALMsg.ERROR_UNKNOWN_HOST, host);
	} catch (Exception e) {
	    log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return false;
    }

    /**
     * Closes the connection to the remote host.  This will cause any open keys to be closed.
     */
    public synchronized void disconnect() {
	try {
	    heartbeat.cancel();
	    Enumeration <Key>keys = map.elements();
	    while (keys.hasMoreElements()) {
		Key key = keys.nextElement();
		if (key.open) {
		    log.getLogger().trace(JOVALMsg.STATUS_WINREG_KEYCLEAN, key.toString());
		    key.close();
		}
	    }
	    winreg.closeConnection();
	    state = STATE_DISCONNECTED;
	} catch (JIException e) {
	    log.getLogger().warn(JOVALMsg.ERROR_WINREG_DISCONNECT);
	    log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement IRegistry

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
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, name));
	}

	if (hive == null) {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_HIVE, name));
	}
	return hive;
    }

    /**
     * Returns a key given a path that includes the hive.
     */
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

    /**
     * Retrieve a Key, first by attempting to retrieve the key from the local cache, then from the deepest (nearest)
     * ancestor key available from the cache.
     */
    public IKey fetchKey(String hive, String path) throws NoSuchElementException {
	return fetchSubkey(getHive(hive), path);
    }

    /**
     * Retrieve a subkey, first by attempting to retrieve the subkey from the local cache, then from the deepest (nearest)
     * ancestor key available from the cache.  This method redirects to the 64-bit portion of the registry if it fails
     * to find the key in the 32-bit portion.
     *
     * @throws NoSuchElementException if there is no subkey with the specified name.
     */
    public IKey fetchSubkey(IKey parent, String name) throws NoSuchElementException {
	String fullPath = new StringBuffer(parent.toString()).append(DELIM_CH).append(name).toString();
	Key key = map.get(fullPath);
	if (key == null) {
	    if (redirector != null) {
		String alt = redirector.getRedirect(fullPath);
		if (alt != null) {
		    log.getLogger().trace(JOVALMsg.STATUS_WINREG_REDIRECT, fullPath, alt);
		    return fetchKey(alt);
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

    /**
     * Thread-safe proxy for IJIWinReg.winreg_OpenKey.
     */
    synchronized JIPolicyHandle wrOpenKey(JIPolicyHandle handle, String name, int mode) throws JIException {
	return winreg.winreg_OpenKey(handle, name, mode);
    }

    /**
     * Thread-safe proxy for IJIWinReg.winreg_CloseKey.
     */
    synchronized void wrCloseKey(JIPolicyHandle handle) throws JIException {
	winreg.winreg_CloseKey(handle);
    }

    /**
     * Thread-safe proxy for IJIWinReg.winreg_EnumKey.
     */
    synchronized String[] wrEnumKey(JIPolicyHandle handle, int n) throws NoSuchElementException {
	try {
	    return winreg.winreg_EnumKey(handle, n);
	} catch (JIException e) {
	    switch(e.getErrorCode()) {
	      case JIErrorCodes.ERROR_NO_MORE_ITEMS:
		throw new NoSuchElementException(e.getMessage());

	      default:
		throw new RuntimeException(e);
	    }
	}
    }

    /**
     * Thread-safe proxy for IJIWinReg.winreg_EnumValue.
     */
    synchronized Object[] wrEnumValue(JIPolicyHandle handle, int n) throws NoSuchElementException {
	try {
	    return winreg.winreg_EnumValue(handle, n);
	} catch (JIException e) {
	    switch(e.getErrorCode()) {
	      case JIErrorCodes.ERROR_NO_MORE_ITEMS:
		throw new NoSuchElementException(e.getMessage());

	      default:
		throw new RuntimeException(e);
	    }
	}
    }

    synchronized Value createValue(Key key, String name) throws IllegalArgumentException, NoSuchElementException {
	Value val = null;

	int len = BUFFER_LEN;
	Object[] oa = null;
	boolean retry = false;

	do {
	    try {
		oa = winreg.winreg_QueryValue(key.handle, name, len);
		retry = false;
	    } catch (JIException e) {
		switch(e.getErrorCode()) {
		  case JIErrorCodes.ERROR_NO_MORE_ITEMS:
		    throw new NoSuchElementException(e.getMessage());

		  case 0x000000EA: // This code appears to mean "insufficient buffer"
		    retry = true;
		    len += len;
		    break;

		  default:
		    throw new RuntimeException(e);
		}
	    }
	} while (retry);

	if (oa.length == 2) {
	    int type = ((Integer)oa[0]).intValue();
	    switch(type) {
	      case IJIWinReg.REG_MULTI_SZ: {
		byte[][] data = (byte[][])oa[1];
		ArrayList<String> sa = new ArrayList<String>(data.length);
		for (int i=0; i < data.length; i++) {
		    String s = getString(data[i]);
		    if (s == null) {
			break; // no more values
		    } else {
			sa.add(s);
		    }
		}
		val = new MultiStringValue(key, name, sa.toArray(new String[0]));
		break;
	      }

	      case IJIWinReg.REG_BINARY: {
		byte[] data = (byte[])oa[1];
		val = new BinaryValue(key, name, data);
		break;
	      }

	      case IJIWinReg.REG_DWORD: {
        	val = new DwordValue(key, name, DwordValue.byteArrayToInt((byte[])oa[1]));
		break;
	      }

	      case IJIWinReg.REG_QWORD: {
        	val = new QwordValue(key, name, QwordValue.byteArrayToLong((byte[])oa[1]));
		break;
	      }

	      case IJIWinReg.REG_EXPAND_SZ: {
		String raw = getString((byte[])oa[1]);
		String expanded = null;
		if (state != STATE_ENV) {
		    expanded = env.expand(raw);
		}
		val = new ExpandStringValue(key, name, raw, expanded);
		break;
	      }

	      case IJIWinReg.REG_SZ:
		val = new StringValue(key, name, getString((byte[])oa[1]));
		break;

	      case IJIWinReg.REG_NONE:
	      default:
		throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_TYPE, type));
	    }
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_QUERYVAL, new Integer(oa.length)));
	}
	log.getLogger().trace(JOVALMsg.STATUS_WINREG_VALINSTANCE, val.toString());
	return val;
    }

    /**
     * All Keys are maintained in a Map with this Registry object so that they can be verified closed when disconnecting.
     * If a Key that is mapped is ever re-fetched, the original Key is closed and replaced in the map with the new copy.
     */
    void registerKey(Key key) {
	String path = key.toString();
	if (map.containsKey(path)) {
	    log.getLogger().warn(JOVALMsg.ERROR_WINREG_REKEY, path);
	    Key k = map.get(path);
	    k.close();
	    map.remove(path);
	}
	map.put(path, key);
	log.getLogger().trace(JOVALMsg.STATUS_WINREG_KEYREG, path);
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
	    log.getLogger().trace(JOVALMsg.STATUS_WINREG_KEYDEREG, path);
	}
    }

    /**
     * Convert bytes to a String using the default encoding (US-ASCII).
     *
     * TBD: i18n
     */
    String getString(byte[] b) {
	String s = null;
	try {
	    if (b != null) {
		s = new String(b, Charset.forName(DEFAULT_ENCODING));
	    }
	} catch (IllegalCharsetNameException e) {
	    log.getLogger().warn(JOVALMsg.ERROR_WINREG_CONVERSION);
	    log.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (UnsupportedCharsetException e) {
	    log.getLogger().warn(JOVALMsg.ERROR_WINREG_CONVERSION);
	    log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return s;
    }

    /**
     * Get the HKEY_LOCAL_MACHINE key.
     */
    synchronized Key getHKLM() {
	if (hklm == null) {
	    try {
		hklm = new Key(this, HKLM, winreg.winreg_OpenHKLM());
	    } catch (JIException e) {
		log.getLogger().error(JOVALMsg.ERROR_WINREG_HIVE_OPEN,  HKLM);
		log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return hklm;
    }

    /**
     * Get the HKEY_USERS key.
     */
    synchronized Key getHKU() {
	if (hku == null) {
	    try {
		hku = new Key(this, HKU, winreg.winreg_OpenHKU());
	    } catch (JIException e) {
		log.getLogger().error(JOVALMsg.ERROR_WINREG_HIVE_OPEN,  HKU);
		log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return hku;
    }

    /**
     * Get the HKEY_CURRENT_USER key.
     */
    synchronized Key getHKCU() {
	if (hkcu == null) {
	    try {
		hkcu = new Key(this, HKCU, winreg.winreg_OpenHKCU());
	    } catch (JIException e) {
		log.getLogger().error(JOVALMsg.ERROR_WINREG_HIVE_OPEN,  HKCU);
		log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return hkcu;
    }

    /**
     * Get the HKEY_CLASSES_ROOT key.
     */
    synchronized Key getHKCR() {
	if (hkcr == null) {
	    try {
		hkcr = new Key(this, HKCR, winreg.winreg_OpenHKCR());
	    } catch (JIException e) {
		log.getLogger().error(JOVALMsg.ERROR_WINREG_HIVE_OPEN,  HKCR);
		log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return hkcr;
    }

    // Private

    class AuthInfo implements IJIAuthInfo {
	IWindowsCredential wc;

	AuthInfo(IWindowsCredential wc) {
	    this.wc = wc;
	}

	// Implement IJIAuthInfo

	public String getUserName() {
	    return wc.getUsername();
	}

	public String getPassword() {
	    return wc.getPassword();
	}

	public String getDomain() {
	    return wc.getDomain();
	}
    }

    class RegistryTask extends TimerTask {
	RegistryTask() {
	    super();
	}

	public void run() {
	    try {
		IKey key = fetchKey(HKLM, WindowsSystemInfo.COMPUTERNAME_KEY);
		IValue val = fetchValue(key, WindowsSystemInfo.COMPUTERNAME_VAL);
	    } catch (NoSuchElementException e) {
		log.getLogger().warn(JOVALMsg.ERROR_WINREG_HEARTBEAT, JOVALSystem.getMessage(JOVALMsg.ERROR_WINREG_KEY_MISSING, e.getMessage()));
	    } catch (Exception e) {
		log.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }
}
