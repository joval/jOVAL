// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

import java.io.*;
import java.util.Properties;

import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.oval.IResults;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.plugin.RemotePlugin;
import org.joval.util.JOVALSystem;

/**
 * A trivial implementation of an OVAL scanner using the jOVAL library.
 *
 * @author David A. Solin
 */
public class TrivialScanner {
    /**
     * The TrivialScanner accepts two command-line arguments.  The first is the path to an XML file containing OVAL
     * definitions, and the second is the path to a properties file containing configuration information for the
     * RemotePlugin class.
     */
    public static void main(String[] argv) {
	try {
	    Properties props = new Properties();
	    props.load(new FileInputStream(new File(argv[1])));
	    RemotePlugin plugin = new RemotePlugin();
	    plugin.setDataDirectory(new File("."));
	    if (plugin.configure(props)) {
		IEngine engine = JOVALSystem.createEngine();
		engine.setDefinitionsFile(new File(argv[0]));
		engine.setPlugin(plugin);
		engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
		engine.run();
		switch(engine.getResult()) {
		  case OK:
		    System.out.println("Writing resutls.xml");
		    IResults results = engine.getResults();
		    results.writeXML(new File("results.xml"));
		    break;
		  case ERR:
		    throw engine.getError();
		}
		System.exit(0);
	    } else {
		System.out.println("Bad plugin configuration: " + plugin.getLastError());
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (OvalException e) {
	    e.printStackTrace();
	}

	System.exit(1);
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
		System.out.println("Saving system-characteristics.xml");
		ISystemCharacteristics sc = (ISystemCharacteristics)arg;
		sc.writeXML(new File("system-characteristics.xml"));
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
}
