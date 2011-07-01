// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.wmi;

import java.util.Hashtable;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.util.JOVALSystem;
import org.joval.windows.wmi.WmiException;

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

    public WmiProvider() {
	map = new Hashtable<String, Dispatch>();
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
	    locator = new ActiveXComponent("WbemScripting.SWbemLocator");
	    return true;
	} catch (UnsatisfiedLinkError e) {
	    return false;
	}
    }

    public void disconnect() {
	if (locator != null) {
	    locator.safeRelease();
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
