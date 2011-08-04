// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.InputStream;

import org.joval.identity.Credential;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.unix.Sudo;

public class Exec {
    ISession session;

    public Exec(ISession session) {
	this.session = session;
    }

    public void test(String command) {
	try {
	    IProcess p = session.createProcess(command);
	    p.start();
	    InputStream in = p.getInputStream();
	    int len = 0;
	    byte[] buff = new byte[1024];
	    while((len = in.read(buff)) > 0) {
		System.out.write(buff, 0, len);
	    }
	    System.out.println("");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
