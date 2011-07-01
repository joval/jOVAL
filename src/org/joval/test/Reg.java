// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.joval.intf.system.ISession;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.system.IWindowsSession;

public class Reg {
    IWindowsSession session;

    public Reg(ISession session) {
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	}
    }

    public void test(String keyName) {
	try {
	    IRegistry r = session.getRegistry();
	    if (r.connect()) {
		IKey cv = r.fetchKey(keyName);
		String[] sa = cv.listValues();
		for (int i=0; i < sa.length; i++) {
		    System.out.println("Value name: " + sa[i] + " val: " + cv.getValue(sa[i]).toString());
		}
		cv.closeAll();
		r.disconnect();
	    } else {
	 	System.out.println("Failed to connect to registry");
	    }
	} catch (NoSuchElementException e) {
	    e.printStackTrace();
	}
    }
}

