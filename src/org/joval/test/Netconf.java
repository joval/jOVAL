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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.vngx.jsch.JSch;

import org.joval.identity.SimpleCredentialStore;
import org.joval.protocol.netconf.NetconfSession;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JSchLogger;

public class Netconf {
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

	    Logger logger = Logger.getLogger(JOVALMsg.getLogger().getName());
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
	    if ("true".equals(props.getProperty("jsch.verbose"))) {
		JSch.setLogger(new JSchLogger(JOVALMsg.getLogger()));
	    }

	    SimpleCredentialStore scs = new SimpleCredentialStore();
	    String host = props.getProperty("host");
	    String gwHost = props.getProperty("gateway.host");
	    if (gwHost != null) {
		Properties p = new Properties();
		p.setProperty(SimpleCredentialStore.PROP_HOSTNAME, gwHost);
		String username = props.getProperty("gateway.username");
		if (username != null) {
		    p.setProperty(SimpleCredentialStore.PROP_USERNAME, username);
		}
		String password = props.getProperty("gateway.password");
		if (password != null) {
		    p.setProperty(SimpleCredentialStore.PROP_PASSWORD, password);
		}
		scs.add(p);
	    }

	    Properties p = new Properties();
	    p.setProperty(SimpleCredentialStore.PROP_HOSTNAME, host);
	    String username = props.getProperty("username");
	    if (username != null) {
		p.setProperty(SimpleCredentialStore.PROP_USERNAME, username);
	    }
	    String password = props.getProperty("password");
	    if (password != null) {
		p.setProperty(SimpleCredentialStore.PROP_PASSWORD, password);
	    }
	    String privateKey = props.getProperty("privateKey");
	    if (privateKey != null) {
		p.setProperty(SimpleCredentialStore.PROP_PRIVATE_KEY, privateKey);
	    }
	    String passphrase = props.getProperty("passphrase");
	    if (passphrase != null) {
		p.setProperty(SimpleCredentialStore.PROP_PASSPHRASE, passphrase);
	    }
	    String rootPassword = props.getProperty("rootPassword");
	    if (rootPassword != null) {
		p.setProperty(SimpleCredentialStore.PROP_ROOT_PASSWORD, rootPassword);
	    }
	    scs.add(p);

	    SshSession gateway = null;
	    if (gwHost != null) {
		gateway = new SshSession(gwHost, null);
		gateway.unlock(scs.getCredential(gateway));
	    }

	    SshSession ssh = new SshSession(host, gateway, null);
	    ssh.unlock(scs.getCredential(ssh));

	    NetconfSession netconf = new NetconfSession(ssh, 5000L);
	    Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    transformer.transform(new DOMSource(netconf.getConfig()), new StreamResult(System.out));

	    ssh.disconnect();
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

