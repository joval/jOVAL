// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.juniper.system.ISupportInformation;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.oval.IResults;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.os.cisco.system.IosSession;
import org.joval.os.cisco.system.TechSupport;
import org.joval.os.juniper.system.JunosSession;
import org.joval.os.juniper.system.SupportInformation;
import org.joval.oval.OvalException;
import org.joval.oval.OvalFactory;
import org.joval.protocol.tftp.TftpURLStreamHandler;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A simple utility for running OVAL definitions against IOS devices using only the output from show tech-support.
 *
 * @author David A. Solin
 */
public class CiscoPlugin extends BasePlugin {
    /**
     * The Cisco utility accepts two command-line arguments.  The first is the path to an XML file containing OVAL
     * definitions, and the second is a URL to information returned from the command "show tech-support" run on a Cisco
     * IOS device.  If a second argument is not supplied, then the data will be read from the standard input.
     */
    public static void main(String[] argv) {
	File definitions = null;
	String input = null;

	if (argv.length == 2) {
	    definitions = new File(argv[0]);
	    input = argv[1];
	} else if (argv.length == 1) {
	    definitions = new File(argv[0]);
	}

	if (definitions == null) {
	    usage();
	    System.exit(1);
	} else if (!definitions.isFile()) {
	    System.out.println("No such file: " + definitions.toString());
	    usage();
	    System.exit(1);
	}

	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("java.util.logging.handlers=".getBytes()));
	    Logger logger = Logger.getLogger(JOVALMsg.getLogger().getName());
	    Handler logHandler = new FileHandler("disco.log", false);
	    logHandler.setFormatter(new SimpleFormatter());
	    logHandler.setLevel(Level.INFO);
	    logger.setLevel(Level.INFO);
	    logger.addHandler(logHandler);

	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    InputStream in;
	    if (input != null) {
		in = toURL(input).openStream();
	    } else {
		in = System.in;
	    }
	    byte[] buff = new byte[1024];
	    int len = 0;
	    while((len = in.read(buff)) > 0) {
		out.write(buff, 0, len);
	    }

	    IPlugin plugin = null;
	    ITechSupport tech = null;
	    tech = new TechSupport(new ByteArrayInputStream(out.toByteArray()));
	    if (tech.getHeadings().size() == 0) {
		ISupportInformation info = new SupportInformation(new ByteArrayInputStream(out.toByteArray()));
		plugin = new CiscoPlugin(info);
	    } else {
		plugin = new CiscoPlugin(tech);
	    }

	    IEngine engine = OvalFactory.createEngine(IEngine.Mode.EXHAUSTIVE, plugin.getSession());
	    engine.setDefinitions(OvalFactory.createDefinitions(new File(argv[0])));
	    engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
	    engine.run();
	    switch(engine.getResult()) {
	      case OK:
		System.out.println("Writing disco-resutls.xml");
		IResults results = engine.getResults();
		results.writeXML(new File("disco-results.xml"));
		break;
	      case ERR:
		throw engine.getError();
	    }
	    System.exit(0);
	} catch (MalformedURLException e) {
	    System.out.println("Not a proper URL or file path: " + argv[1]);
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    /**
     * Convert a string representing a regular URL, TFTP URL, or file path to a URL.
     */
    public static URL toURL(String str) throws MalformedURLException, SecurityException {
	if (str.startsWith("tftp:")) {
	    return new URL(null, str, new TftpURLStreamHandler());
	}

	MalformedURLException ex = null;
	try {
	    return new URL(str);
	} catch (MalformedURLException e) {
	    ex = e;
	}

	File f = new File(str);
	if (f.isFile()) {
	    return f.toURI().toURL();
	}
	throw ex;
    }

    static void usage() {
	System.out.println("jOVAL(TM) Disco utility: An offline OVAL evaluator for Cisco IOS devices.");
	System.out.println("Copyright(C) 2012, jOVAL.org.  All rights reserved.");
	System.out.println("");
	System.out.println("Usage:");
	System.out.println("  disco [defs] [input]");
	System.out.println("");
	System.out.println("Arguments:");
	System.out.println("  [defs]  = Required argument specifying the path to an XML file containing");
	System.out.println("            OVAL definitions.");
	System.out.println("  [input] = Optional argument specifying the URL or path to a file containing");
	System.out.println("            the contents of the IOS command \"show tech-support\".  If not");
	System.out.println("            specified, data is read from the standard input.");
    }

    /**
     * An inner class that prints out information about Engine notifications.
     */
    static class Observer implements IObserver {
	public Observer() {}
    
	public void notify(IProducer source, int msg, Object arg) {
	    switch(msg) {
	      case IEngine.MESSAGE_OBJECT_PHASE_START:
		System.out.println("Scanning objects...");
		break;
	      case IEngine.MESSAGE_OBJECT:
		System.out.println("  " + (String)arg);
		break;
	      case IEngine.MESSAGE_OBJECT_PHASE_END:
		System.out.println("Done scanning");
		break;
	      case IEngine.MESSAGE_SYSTEMCHARACTERISTICS:
		System.out.println("Saving system-characteristics to disco-sc.xml");
		ISystemCharacteristics sc = (ISystemCharacteristics)arg;
		sc.writeXML(new File("disco-sc.xml"));
		break;
	      case IEngine.MESSAGE_DEFINITION_PHASE_START:
		System.out.println("Evaluating definitions...");
		break;
	      case IEngine.MESSAGE_DEFINITION:
		System.out.println("  " + (String)arg);
		break;
	      case IEngine.MESSAGE_DEFINITION_PHASE_END:
		System.out.println("Done evaluating definitions");
		break;
	      default:
		System.out.println("Unexpected message: " + msg);
		break;
	    }
	}
    }

    // Implement IPlugin

    public CiscoPlugin() {
	super();
    }

    @Override
    public void configure(Properties props) throws Exception {
	super.configure(props);

	String str = props.getProperty("tech.url");
	if (str == null) {
	    throw new Exception(getMessage("err.configPropMissing", "tech.url"));
	}
	URL url = CiscoPlugin.toURL(props.getProperty("tech.url"));
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	InputStream in = url.openStream();
	byte[] buff = new byte[1024];
	int len = 0;
	while((len = in.read(buff)) > 0) {
	    out.write(buff, 0, len);
	}

	ITechSupport tech = new TechSupport(new ByteArrayInputStream(out.toByteArray()));
	if (tech.getHeadings().size() == 0) {
	    session = new JunosSession(new SupportInformation(new ByteArrayInputStream(out.toByteArray())));
	} else {
	    session = new IosSession(tech);
	}
    }

    // Private

    private CiscoPlugin(ITechSupport tech) {
	super();
	session = new IosSession(tech);
    }

    private CiscoPlugin(ISupportInformation info) {
	super();
	session = new JunosSession(info);
    }
}

