// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi;

import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.impls.automation.IJIDispatch;

import com.h9labs.jwbem.SWbemLocator;
import com.h9labs.jwbem.SWbemObject;
import com.h9labs.jwbem.SWbemObjectSet;
import com.h9labs.jwbem.SWbemServices;

import org.joval.intf.identity.IWindowsCredential;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.remote.wmi.query.SimpleSWbemObjectSet;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A thin wrapper class around the JWbem packages that maintains one SWbemServices per namespace associated with
 * a host, for performance and abstraction purposes.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiConnection implements IWmiProvider {
    private SWbemLocator locator;
    private String host;
    private IWindowsCredential cred;
    private Hashtable <String, SWbemServices>map;

    public WmiConnection(String host, IWindowsCredential cred) {
	this.host = host;
	this.cred = cred;
    }

    public boolean connect() {
	if (locator == null) {
	    JOVALSystem.getLogger().info(JOVALMsg.STATUS_WMI_CONNECT);
	    map = new Hashtable <String, SWbemServices>();
	    locator = new SWbemLocator(); 
	}
	return true;
    }

    public void disconnect() {
	if (locator != null) {
	    JOVALSystem.getLogger().info(JOVALMsg.STATUS_WMI_DISCONNECT);
	    locator.disconnect();
	    locator = null;
	}
	map.clear();
    }

    /**
     * Execute a query on another host, using the locator on the connected server as a proxy.
     */
    public SWbemObjectSet <SWbemObject>execQuery(String target, String ns, String wql)
		throws UnknownHostException, JIException {

	return getServices(target, ns).execQuery(wql);
    }

    public SWbemServices getServices(String target, String namespace) throws UnknownHostException, JIException {
	String key = new StringBuffer(target).append(":").append(namespace).toString();
	SWbemServices services = map.get(key);
	if (services == null) {
	    services = locator.connect(host, target, namespace, cred.getDomainUser(), cred.getPassword());
	    map.put(key, services);
	}
	return services;
    }

    // Implement IWmiProvider

    /**
     * Execute a query on the host.
     */
    public ISWbemObjectSet execQuery(String ns, String wql) throws WmiException {
	try {
	    return new SimpleSWbemObjectSet(getServices(host, ns).execQuery(wql));
	} catch (UnknownHostException e) {
	    throw new WmiException(e);
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
}
