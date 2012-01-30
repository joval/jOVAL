// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.oval.IResults;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.os.embedded.system.IosSession;
import org.joval.os.embedded.system.TechSupport;
import org.joval.oval.OvalException;
import org.joval.plugin.adapter.cisco.ios.GlobalAdapter;
import org.joval.plugin.adapter.cisco.ios.InterfaceAdapter;
import org.joval.plugin.adapter.cisco.ios.LineAdapter;
import org.joval.plugin.adapter.cisco.ios.SnmpAdapter;
import org.joval.plugin.adapter.cisco.ios.TclshAdapter;
import org.joval.plugin.adapter.cisco.ios.VersionAdapter;
import org.joval.plugin.adapter.cisco.ios.Version55Adapter;
import org.joval.plugin.adapter.independent.FamilyAdapter;
import org.joval.plugin.adapter.independent.VariableAdapter;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A simple utility for running OVAL definitions against IOS devices using only the output from show tech-support.
 *
 * @author David A. Solin
 */
public class CiscoPlugin implements IPlugin {
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
	    LogManager.getLogManager().readConfiguration(cl.getResourceAsStream("disco.logging.properties"));
	    Logger logger = Logger.getLogger(JOVALSystem.getLogger().getName());
	    Handler logHandler = new FileHandler("disco.log", false);
	    logHandler.setFormatter(new SimpleFormatter());
	    logHandler.setLevel(Level.INFO);
	    logger.setLevel(Level.INFO);
	    logger.addHandler(logHandler);

	    TechSupport tech = null;
	    if (input != null) {
		try {
		    URL url = new URL(input);
		    tech = new TechSupport(url.openStream());
		} catch (MalformedURLException e) {
		    File f = new File(input);
		    if (f.isFile()) {
			tech = new TechSupport(new FileInputStream(f));
		    } else {
			throw e;
		    }
		}
	    } else {
		tech = new TechSupport(System.in);
	    }

	    CiscoPlugin plugin = new CiscoPlugin(tech);
	    IEngine engine = JOVALSystem.createEngine(plugin);
	    engine.setDefinitionsFile(new File(argv[0]));
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
	} catch (OvalException e) {
	    e.printStackTrace();
	}

	System.exit(1);
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
	System.out.println("            the contents of the IOS command \"show tech-support\".  If not,");
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

    private LocLogger logger;
    private IosSession session;
    private Collection<IAdapter> adapters;

    public CiscoPlugin(TechSupport techSupport) {
	logger = JOVALSystem.getLogger();
	session = new IosSession(techSupport);

	adapters = new Vector<IAdapter>();
	adapters.add(new FamilyAdapter(session));
	adapters.add(new VariableAdapter());
	adapters.add(new GlobalAdapter(session));
	adapters.add(new InterfaceAdapter(session));
	adapters.add(new LineAdapter(session));
	adapters.add(new SnmpAdapter(session));
	adapters.add(new VersionAdapter(session));
	adapters.add(new Version55Adapter(session));
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
	if (session != null) {
	    session.setLogger(logger);
	}
    }

    // Implement IPlugin

    public Collection<IAdapter> getAdapters() {
	return adapters;
    }

    public void connect() throws OvalException {
    }

    public void disconnect() {
    }

    public SystemInfoType getSystemInfo() {
	return session.getSystemInfo();
    }
}

