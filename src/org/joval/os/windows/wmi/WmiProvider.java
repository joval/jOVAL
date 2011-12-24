// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.wmi;

import java.util.Hashtable;

import org.slf4j.cal10n.LocLogger;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A simple class for performing WMI queries.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiProvider implements IWmiProvider {
    private static boolean libLoaded = false;

    private ActiveXComponent locator;
    private Hashtable <String, Dispatch>map;
    private ILoggable log;

    public WmiProvider(ILoggable log) {
	this.log = log;
	map = new Hashtable<String, Dispatch>();
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return log.getLogger();
    }

    public void setLogger(LocLogger logger) {
	log.setLogger(logger);
    }

    // Implement ISWbemProvider

    public boolean connect() {
	try {
	    if (!libLoaded) {
		if ("32".equals(System.getProperty("sun.arch.data.model"))) {
		    System.loadLibrary("jacob-1.15-M4-x86");
		} else {
		    System.loadLibrary("jacob-1.15-M4-x64");
		}
	        libLoaded = true;
	    }
	    if (locator == null) {
		log.getLogger().info(JOVALMsg.STATUS_WMI_CONNECT);
		locator = new ActiveXComponent("WbemScripting.SWbemLocator");
	    }
	    return true;
	} catch (UnsatisfiedLinkError e) {
	    log.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public void disconnect() {
	if (locator != null) {
	    log.getLogger().info(JOVALMsg.STATUS_WMI_DISCONNECT);
	    locator.safeRelease();
	    locator = null;
	}
	map.clear();
    }

    public ISWbemObjectSet execQuery(String ns, String wql) throws WmiException {
	Dispatch services = map.get(ns);
	if (services == null) {
	    services = locator.invoke("ConnectServer", Variant.DEFAULT, new Variant(ns)).toDispatch();
	    map.put(ns, services);
	}
	return new SWbemObjectSet(Dispatch.call(services, "ExecQuery", wql).toDispatch());
    }
}
