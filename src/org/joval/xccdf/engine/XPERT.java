// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joval.cpe.CpeException;
import org.joval.intf.plugin.IPlugin;
import org.joval.oval.OvalException;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT {
    private static File BASE_DIR = new File(".");
    private static PropertyResourceBundle resources;
    static {
	String s = System.getProperty("xpert.baseDir");
	if (s != null) {
	    BASE_DIR = new File(s);
	}
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("xpert.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("xpert.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("xpert.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    static Logger logger;
    static final File ws = new File("artifacts");

    static void printHeader(IPlugin plugin) {
	PrintStream console = System.out;
	console.println(getMessage("divider"));
	console.println(getMessage("product.name"));
	console.println(getMessage("description"));
	console.println(getMessage("message.version", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION)));
	console.println(getMessage("message.buildDate", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_BUILD_DATE)));
	console.println(getMessage("copyright"));
	if (plugin != null) {
	    console.println("");
	    console.println(getMessage("message.plugin.name", plugin.getProperty(IPlugin.PROP_DESCRIPTION)));
	    console.println(getMessage("message.plugin.version", plugin.getProperty(IPlugin.PROP_VERSION)));
	    console.println(getMessage("message.plugin.copyright", plugin.getProperty(IPlugin.PROP_COPYRIGHT)));
	}
	console.println(getMessage("divider"));
	console.println("");
    }

    static void printHelp(IPlugin plugin) {
	System.out.println(getMessage("helpText"));
	if (plugin != null) {
	    System.out.println(plugin.getProperty(IPlugin.PROP_HELPTEXT));
	}
    }

    /**
     * Retrieve a message using its key.
     */
    static String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    /**
     * Run from the command-line.
     */
    public static void main(String[] argv) {
	int exitCode = 1;
	if (!ws.exists()) {
	    ws.mkdir();
	}
	try {
	    logger = LogFormatter.createDuplex(new File(ws, "xpert.log"), Level.INFO);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

	String pluginName = "default";
	String configFileName = "config.properties";
	String profileName = null;
	String xccdfBaseName = null;
	boolean printHelp = false;
	boolean debug = false;

	for (int i=0; i < argv.length; i++) {
	    if (argv[i].equals("-h")) {
		printHelp = true;
	    } else if (i == (argv.length - 1)) { // last arg (but not -h)
		xccdfBaseName = argv[i];
	    } else if (argv[i].equals("-config")) {
		configFileName = argv[++i];
	    } else if (argv[i].equals("-profile")) {
		profileName = argv[++i];
	    } else if (argv[i].equals("-plugin")) {
		pluginName = argv[++i];
	    } else if (argv[i].equals("-debug")) {
		debug = true;
	    }
	}

	IPlugin plugin = null;
	try {
	    plugin = PluginFactory.newInstance(new File(BASE_DIR, "plugin")).createPlugin(pluginName);
	} catch (IllegalArgumentException e) {
	    logger.severe("Not a directory: " + e.getMessage());
	} catch (NoSuchElementException e) {
	    logger.severe("Plugin not found: " + e.getMessage());
	} catch (PluginConfigurationException e) {
	    logger.severe(LogFormatter.toString(e));
	}

	printHeader(plugin);
	if (printHelp) {
	    printHelp(plugin);
	    exitCode = 0;
	} else if (xccdfBaseName == null) {
	    logger.warning("No XCCDF file was specified");
	    printHelp(plugin);
	} else if (plugin == null) {
	    printHelp(null);
	} else {
	    logger.info("Start time: " + new Date().toString());
	    try {
		//
		// Configure the jOVAL plugin
		//
		Properties config = new Properties();
		File configFile = new File(configFileName);
		if (configFile.isFile()) {
		    config.load(new FileInputStream(configFile));
		}
		plugin.configure(config);
	    } catch (Exception e) {
		logger.severe("Problem configuring the plugin -- check that the configuration is valid");
		System.exit(1);
	    }

	    try {
		//
		// Load the XCCDF and selected profile
		//
		logger.info("Loading " + xccdfBaseName);
		XccdfBundle xccdf = new XccdfBundle(new File(xccdfBaseName));
		Profile profile = new Profile(xccdf, profileName);

		//
		// Perform the evaluation
		//
		Engine engine = new Engine(xccdf, profile, plugin.getSession(), debug);
		engine.run();
		logger.info("Finished processing XCCDF bundle");
		exitCode = 0;
	    } catch (CpeException e) {
		logger.severe(LogFormatter.toString(e));
	    } catch (OvalException e) {
		logger.severe(LogFormatter.toString(e));
	    } catch (XccdfException e) {
		logger.severe(LogFormatter.toString(e));
	    } catch (Exception e) {
		logger.severe(LogFormatter.toString(e));
	    }
	}

	System.exit(exitCode);
    }
}
