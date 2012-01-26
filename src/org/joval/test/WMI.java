// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.net.UnknownHostException;
import java.util.Iterator;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.os.windows.wmi.WmiException;

public class WMI {
    IWindowsSession session;

    public WMI(IBaseSession session) {
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	}
    }

    public synchronized void test(String ns, String wql) {
	try {
	    IWmiProvider provider = session.getWmiProvider();
	    if (provider.connect()) {
		ISWbemObjectSet objSet = provider.execQuery(ns, wql);
		System.out.println("Objects: " + objSet.getSize());
		Iterator <ISWbemObject>iter = objSet.iterator();
		while (iter.hasNext()) {
		    ISWbemObject obj = iter.next();
		    System.out.println("Object");
		    ISWbemPropertySet props = obj.getProperties();
		    Iterator <ISWbemProperty>propIter = props.iterator();
		    while (propIter.hasNext()) {
			ISWbemProperty prop = propIter.next();
			System.out.println("  " + prop.getName() + "=" + prop.getValue() +
					   ", Class: " + prop.getValue().getClass().getName());
		    }
		}
		provider.disconnect();
	    }
	} catch (WmiException e) {
	    e.printStackTrace();
	}
    }
}
