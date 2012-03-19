// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.IOException;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.ISession;
import org.joval.os.windows.pe.Header;
import org.joval.util.JOVALMsg;

public class PE {
    ISession session;

    public PE(ISession session) {
	this.session = session;
    }

    public void test(String path) {
	System.out.println("Scanning " + path);
	IFilesystem fs = session.getFilesystem();
	try {
	    Header header = new Header(fs.getFile(path), JOVALMsg.getLogger());
	    header.debugPrint(System.out);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
