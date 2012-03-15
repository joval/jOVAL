// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.InputStream;

import org.joval.identity.Credential;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IProcess;

public class Exec {
    IBaseSession session;

    public Exec(IBaseSession session) {
	this.session = session;
    }

    public void test(String command) {
	try {
	    String[] env = {"DAS=jOVAL"};
	    IProcess p = session.createProcess(command, env);
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
