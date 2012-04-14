// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi;

import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

import com.h9labs.jwbem.SWbemLocator;
import com.h9labs.jwbem.SWbemServices;

import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.intf.windows.wmi.ISWbemEventSource;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.remote.wmi.scripting.SWbemObjectSet;
import org.joval.os.windows.remote.wmi.scripting.SWbemEventSource;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;

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
    private LocLogger logger;
    private Hashtable <String, SWbemServices>map;

    public WmiConnection(String host, IWindowsCredential cred, ILoggable log) {
	this.host = host;
	this.cred = cred;
	logger = log.getLogger();
    }

    public boolean connect() {
	if (locator == null) {
	    logger.info(JOVALMsg.STATUS_WMI_CONNECT);
	    map = new Hashtable <String, SWbemServices>();
	    locator = new SWbemLocator(); 
	}
	return true;
    }

    public void disconnect() {
	if (locator != null) {
	    logger.info(JOVALMsg.STATUS_WMI_DISCONNECT);
	    locator.disconnect();
	    locator = null;
	}
	map.clear();
    }

    public SWbemServices getServices(String target, String namespace) throws UnknownHostException, JIException {
	String key = new StringBuffer(target).append(":").append(namespace).toString();
	SWbemServices services = map.get(key);
	if (services == null) {
	    connect();
	    services = locator.connect(host, target, namespace, cred.getDomainUser(), cred.getPassword());
	    map.put(key, services);
	}
	return services;
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IWmiProvider

    /**
     * Execute a query on the host.
     */
    public ISWbemObjectSet execQuery(String ns, String wql) throws WmiException {
	try {
	    logger.debug(JOVALMsg.STATUS_WMI_QUERY, host, ns, wql);
	    return new SWbemObjectSet(getServices(host, ns).execQuery(wql));
	} catch (UnknownHostException e) {
	    throw new WmiException(e);
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public ISWbemEventSource execNotificationQuery(String ns, String wql) throws WmiException {
	Object[] inParams = new Object[4];
	inParams[0] = new JIString(wql);
	inParams[1] = JIVariant.OPTIONAL_PARAM();
	inParams[2] = JIVariant.OPTIONAL_PARAM();
	inParams[3] = JIVariant.OPTIONAL_PARAM();
	try {
	    SWbemServices services = getServices(host, ns);
	    JIVariant[] results = services.getObjectDispatcher().callMethodA("ExecNotificationQuery", inParams);
	    IJIComObject co = results[0].getObjectAsComObject();
	    IJIDispatch dispatch = (IJIDispatch)JIObjectFactory.narrowObject(co);
	    return new SWbemEventSource(dispatch, services);
	} catch (UnknownHostException e) {
	    throw new WmiException(e);
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
}
