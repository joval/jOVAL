// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.system.IWindowsSession;

public class Powershell {
    IWindowsSession session;

    public Powershell(IBaseSession session) {
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	}
    }

    public void test(String command) {
	try {
	    IRunspace rs = session.getRunspacePool().spawn();
	    System.out.println("Powershell prompt: " + rs.getPrompt());
	    System.out.println(rs.invoke(command));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}

