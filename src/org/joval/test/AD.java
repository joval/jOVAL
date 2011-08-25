// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.util.NoSuchElementException;

import org.joval.identity.windows.ActiveDirectory;
import org.joval.identity.windows.User;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.windows.wmi.WmiException;

public class AD {
    IWindowsSession session;

    public AD(ISession session) {
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	}
    }

    public void test(String name) {
	try {
	    IWmiProvider wmi = session.getWmiProvider();
	    if (wmi.connect()) {
		ActiveDirectory ad = new ActiveDirectory(wmi);
		User user = ad.queryUser(name);
		System.out.println("Name: " + name);
		System.out.println("SID: " + user.getSid());
		System.out.println("Enabled: " + user.isEnabled());
		for (String group : user.getGroupNetbiosNames()) {
		    System.out.println("Group: " + group);
		}
		wmi.disconnect();
	    } else {
	 	System.out.println("Failed to connect to WMI");
	    }
	} catch (NoSuchElementException e) {
	    System.out.println("User " + name + " not found.");
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (WmiException e) {
	    e.printStackTrace();
	}
    }
}

