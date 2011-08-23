// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.joval.discovery.Local;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALSystem;

public class Default {
    public static void main (String[] argv) {
	try {
	    Properties props = new Properties();
	    if (argv.length == 1) {
		File f = new File(argv[0]);
		FileInputStream in = new FileInputStream(f);
		props.load(in);
		in.close();
	    } else {
		System.exit(1);
	    }

	    if ("true".equals(props.getProperty("joval.verbose"))) {
		JOVALSystem.setInBuiltLogHandler(true);
		JOVALSystem.getLogger().setLevel(Level.FINEST);
	    } else {
		JOVALSystem.getLogger().setLevel(Level.WARNING);
	    }

	    ISession session = Local.getSession();
	    if (session.connect()) {
		if ("true".equals(props.getProperty("test.ad"))) {
		    new AD(session).test(props.getProperty("ad.user"));
		}
		if ("true".equals(props.getProperty("test.registry"))) {
		    new Reg(session).test(props.getProperty("registry.key"));
		}
		if ("true".equals(props.getProperty("test.exec"))) {
		    new Exec(session).test(props.getProperty("exec.command"));
		}
		if ("true".equals(props.getProperty("test.fs"))) {
		    new FS(session).test(props.getProperty("fs.path"));
		}
		if ("true".equals(props.getProperty("test.wmi"))) {
		    new WMI(session).test(props.getProperty("wmi.namespace"), props.getProperty("wmi.query"));
		}
		if ("true".equals(props.getProperty("test.pe"))) {
		    new PE(session).test(props.getProperty("pe.file"));
		}
		session.disconnect();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	System.exit(0);
    }
}

