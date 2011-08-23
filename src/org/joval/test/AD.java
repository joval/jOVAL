// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.windows.UserItem;

import org.joval.identity.windows.ActiveDirectory;
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
		UserItem user = ad.queryUser(name);
		System.out.println("Name: " + name);
		switch(user.getStatus()) {
		  case EXISTS:
		    System.out.println("Enabled: " + (String)user.getEnabled().getValue());
		    for (EntityItemStringType group : user.getGroup()) {
			System.out.println("Group: " + (String)group.getValue());
		    }
		    break;

		  default:
		    System.out.println(user.getStatus());
		    break;
		}
		wmi.disconnect();
	    } else {
	 	System.out.println("Failed to connect to WMI");
	    }
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (WmiException e) {
	    e.printStackTrace();
	}
    }
}

