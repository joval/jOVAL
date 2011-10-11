// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.util.NoSuchElementException;

import org.joval.intf.system.ISession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;

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
		IDirectory ad = session.getDirectory();
		try {
		    IUser user = ad.queryUser(name);
		    System.out.println("User Name: " + name);
		    System.out.println("SID: " + user.getSid());
		    System.out.println("Enabled: " + user.isEnabled());
		    for (String group : user.getGroupNetbiosNames()) {
			System.out.println("Group: " + group);
		    }
		} catch (NoSuchElementException e) {
		    System.out.println("User " + name + " not found.");
		}
		try {
		    IGroup group = ad.queryGroup(name);
		    System.out.println("Group Name: " + name);
		    System.out.println("SID: " + group.getSid());
		    for (String userMember : group.getMemberUserNetbiosNames()) {
			System.out.println("Member User: " + userMember);
		    }
		    for (String groupMember : group.getMemberGroupNetbiosNames()) {
			System.out.println("Member Group: " + groupMember);
		    }
		} catch (NoSuchElementException e) {
		    System.out.println("Group " + name + " not found.");
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

