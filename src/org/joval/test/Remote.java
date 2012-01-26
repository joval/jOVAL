// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.*;

import org.vngx.jsch.JSch;
import org.jinterop.dcom.common.JISystem;

import org.joval.discovery.SessionFactory;
import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.unix.remote.system.UnixSession;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.ssh.identity.SshCredential;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALSystem;
import org.joval.util.JSchLogger;

public class Remote {
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

	    Logger logger = Logger.getLogger(JOVALSystem.getLogger().getName());
	    if ("true".equals(props.getProperty("joval.verbose"))) {
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.FINEST);
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new TestFormatter());
		handler.setLevel(Level.FINEST);
		logger.addHandler(handler);
	    } else {
		logger.setLevel(Level.WARNING);
	    }
	    if ("true".equals(props.getProperty("jinterop.verbose"))) {
		JISystem.setInBuiltLogHandler(true);
		JISystem.getLogger().setLevel(Level.FINEST);
	    } else {
		JISystem.getLogger().setLevel(Level.WARNING);
	    }
	    if ("true".equals(props.getProperty("jsch.verbose"))) {
		JSch.setLogger(new JSchLogger(JOVALSystem.getLogger()));
	    }

	    SshSession gateway = null;
	    String gwHost = props.getProperty("gateway.host");
	    if (gwHost != null) {
		gateway = new SshSession(gwHost);
		String username = props.getProperty("gateway.username");
		String password = props.getProperty("gateway.password");
		if (username != null) {
		    gateway.unlock(new Credential(username, password));
		}
	    }

	    String host = props.getProperty("host");
	    String domain = props.getProperty("domain");
	    String username = props.getProperty("username");
	    String password = props.getProperty("password");
	    String privateKey = props.getProperty("privateKey");
	    String passphrase = props.getProperty("passphrase");
	    String rootPassword = props.getProperty("rootPassword");

	    SessionFactory factory = new SessionFactory(new File("."), gateway);
	    IBaseSession base = factory.createSession(host);
	    IBaseSession session = null;
	    ICredential cred = null;
	    if (base instanceof ILocked) {
		ILocked locked = (ILocked)base;
		switch(base.getType()) {
		  case SSH:
		    if (privateKey != null) {
			cred = new SshCredential(username, new File(privateKey), passphrase, rootPassword);
		    } else {
			cred = new SshCredential(username, password, rootPassword);
		    }
		    break;

		  case WINDOWS:
		    cred = new WindowsCredential(domain, username, password);
		    break;

		  default:
		    cred = new Credential(username, password);
		    break;
		}
		locked.unlock(cred);
	    }
	    ISession.Type type = base.getType();
	    switch(type) {
	      case UNIX:
		UnixSession us = new UnixSession((SshSession)base);
		us.unlock(cred);
		session = us;
		break;

	      case CISCO_IOS:
		base.disconnect();
		IosSession is = new IosSession(new SshSession(host));
		is.unlock(cred);
		session = is;
		break;

	      case WINDOWS:
		session = base;
		break;

	      default:
		System.out.println("Bad type: " + type);
	    }
	    if (session.connect()) {
		if ("true".equals(props.getProperty("test.ad"))) {
		    AD ad = new AD(session);
		    ad.test(props.getProperty("ad.user"));
		}
		if ("true".equals(props.getProperty("test.fs"))) {
		    FS fs = new FS(session);
		    fs.test(props.getProperty("fs.path"));
		}
		if ("true".equals(props.getProperty("test.exec"))) {
		    Exec exec = new Exec(session);
		    exec.test(props.getProperty("exec.command"));
		}
		if ("true".equals(props.getProperty("test.registry"))) {
		    Reg reg = new Reg(session);
		    reg.test(props.getProperty("registry.key"), props.getProperty("registry.value"));
		}
		if ("true".equals(props.getProperty("test.wmi"))) {
		    WMI wmi = new WMI(session);
		    wmi.test(props.getProperty("wmi.namespace"), props.getProperty("wmi.query"));
		}
		session.disconnect();
	    } else {
		System.out.println("Failed to connect the session: " + session.getClass().getName());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	System.exit(0);
    }

    private static class TestFormatter extends Formatter {
        public String format(LogRecord record) {
            StringBuffer line = new StringBuffer(new Date().toString()).append(" - ").append(record.getMessage());
            line.append('\n');
            return line.toString();
        }
    }
}

