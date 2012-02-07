// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import org.joval.intf.plugin.IPlugin;

/**
 * The ExecutionState is responsible for parsing the command-line arguments, and providing data about the user's choices
 * to the Main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ExecutionState {
    static final String DEFAULT_DEFINITIONS		= "definitions.xml";
    static final String DEFAULT_DATA			= "system-characteristics.xml";
    static final String DEFAULT_DIRECTIVES		= "directives.xml";
    static final String DEFAULT_RESULTS_XML		= "results.xml";
    static final String DEFAULT_RESULTS_XFORM		= "results.html";
    static final String DEFAULT_VARIABLES		= "external-variables.xml";
    static final String DEFAULT_LOGFILE			= "jovaldi.log";
    static final String DEFAULT_XMLDIR			= "xml";
    static final String DEFAULT_XFORM			= "results_to_html.xsl";
    static final String DEFAULT_DEFS_SCHEMATRON		= "oval-definitions-schematron.xsl";
    static final String DEFAULT_SC_SCHEMATRON		= "oval-system-characteristics-schematron.xsl";
    static final String DEFAULT_RESULTS_SCHEMATRON	= "oval-results-schematron.xsl";
    static final String DEFAULT_PLUGIN			= "default";

    static File BASE_DIR = new File(".");
    static{
	String s = System.getProperty("INSTALL_DIR");
	if (s != null) {
	    BASE_DIR = new File(s);
	}
    }

    File inputFile;
    File variablesFile;
    File defsFile;
    File inputDefsFile;
    File directivesFile;
    File logFile;
    File xmlDir;
    private File xmlTransform;
    private File schematronDefsXform;
    private File schematronSCXform;
    private File schematronResultsXform;

    List<String> definitionIDs;
    String specifiedChecksum;

    URLClassLoader pluginClassLoader;
    IPluginContainer container;
    Properties pluginConfig = null;

    File dataFile;
    File resultsXML;
    File resultsTransform;

    boolean computeChecksum;
    boolean validateChecksum;
    boolean applyTransform;
    boolean schematronDefs;
    boolean schematronSC;
    boolean schematronResults;
    boolean printLogs;
    boolean printHelp;

    Level logLevel;

    // Internal

    /**
     * Create a new ExecutionState configured with all the default behaviors.
     */
    ExecutionState() {
	//
	// Inputs
	//
	inputFile = null;
	definitionIDs = null;
	specifiedChecksum = null;
	defsFile = new File(BASE_DIR, DEFAULT_DEFINITIONS);
	inputDefsFile = null;
	directivesFile = new File(BASE_DIR, DEFAULT_DIRECTIVES);
	variablesFile = new File(BASE_DIR, DEFAULT_VARIABLES);
	logFile = new File(BASE_DIR, DEFAULT_LOGFILE);
	xmlDir = new File(BASE_DIR, DEFAULT_XMLDIR);
	xmlTransform = null;
	schematronDefsXform = null;
	logLevel = Level.INFO;

	//
	// Outputs
	//
	dataFile = new File(BASE_DIR, DEFAULT_DATA);
	resultsXML = new File(BASE_DIR, DEFAULT_RESULTS_XML);
	resultsTransform = new File(BASE_DIR, DEFAULT_RESULTS_XFORM);

	//
	// Behaviors
	//
	computeChecksum = false;
	validateChecksum = true;
	applyTransform = true;
	schematronDefs = false;
	printLogs = false;
	printHelp = false;
    }

    File getXMLTransform() {
	if (xmlTransform == null) {
	    return new File(xmlDir, DEFAULT_XFORM);
	} else {
	    return xmlTransform;
	}
    }

    File getDefsSchematron() {
	if (schematronDefsXform == null) {
	    return new File(xmlDir, DEFAULT_DEFS_SCHEMATRON);
	} else {
	    return schematronDefsXform;
	}
    }

    File getSCSchematron() {
	if (schematronSCXform == null) {
	    return new File(xmlDir, DEFAULT_SC_SCHEMATRON);
	} else {
	    return schematronSCXform;
	}
    }

    File getResultsSchematron() {
	if (schematronResultsXform == null) {
	    return new File(xmlDir, DEFAULT_RESULTS_SCHEMATRON);
	} else {
	    return schematronResultsXform;
	}
    }

    IPlugin getPlugin() {
	IPlugin plugin = null;
	if (container != null) {
	    plugin = container.getPlugin();
	}
	return plugin;
    }

/* DAS
    Properties getPluginConfig() {
	return new Properties();
    }
*/

    /**
     * Process the command-line arguments.
     *
     * @returns true if successful, false if there is a problem with the arguments.
     */
    boolean processArguments(String[] argv) {
	for (int i=0; i < argv.length; i++) {
	    if (argv[i].equals("-h")) {
		printHelp = true;
		return true;
	    } else if (argv[i].equals("-o")) {
		defsFile = new File(argv[++i]);
	    } else if (argv[i].equals("-z")) {
		computeChecksum = true;
	    } else if (argv[i].equals("-l")) {
		try {
		    switch(Integer.parseInt(argv[++i])) {
		      case 1:
			logLevel = Level.FINEST;
			break;
		      case 2:
			logLevel = Level.INFO;
			break;
		      case 3:
			logLevel = Level.WARNING;
			break;
		      case 4:
			logLevel = Level.SEVERE;
			break;
		      default:
			Main.print(Main.getMessage("ERROR_INVALID_LOG_LEVEL", argv[i]));
			return false;
		    }
		} catch (NumberFormatException e) {
		    Main.print(Main.getMessage("ERROR_INVALID_LOG_LEVEL", e.getMessage()));
		    return false;
		}
	    } else if (argv[i].equals("-p")) {
		printLogs = true;
	    } else if (argv[i].equals("-y")) {
		logFile = new File(argv[++i]);
	    } else if (argv[i].equals("-v")) {
		variablesFile = new File(argv[++i]);
	    } else if (argv[i].equals("-e")) {
		definitionIDs = new Vector<String>();
		StringTokenizer tok = new StringTokenizer(argv[++i], ",");
		while(tok.hasMoreTokens()) {
		    definitionIDs.add(tok.nextToken());
		}
	    } else if (argv[i].equals("-f")) {
		inputDefsFile = new File(argv[++i]);
	    } else if (argv[i].equals("-a")) {
		File temp = new File(argv[++i]);
		if (temp.isDirectory()) {
		    xmlDir = temp;
		} else {
		    Main.print(Main.getMessage("ERROR_INVALID_SCHEMADIR", temp.toString()));
		    return false;
		}
	    } else if (argv[i].equals("-i")) {
		inputFile = new File(argv[++i]);
		container = null;
	    } else if (argv[i].equals("-d")) {
		dataFile = new File(argv[++i]);
	    } else if (argv[i].equals("-g")) {
		directivesFile = new File(argv[++i]);
	    } else if (argv[i].equals("-r")) {
		resultsXML = new File(argv[++i]);
	    } else if (argv[i].equals("-s")) {
		applyTransform = false;
	    } else if (argv[i].equals("-t")) {
		xmlTransform = new File(argv[++i]);
	    } else if (argv[i].equals("-x")) {
		resultsTransform = new File(argv[++i]);
	    } else if (argv[i].equals("-m")) {
		validateChecksum = false;
	    } else if (argv[i].equals("-c")) {
		schematronDefs = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronDefsXform = new File(argv[++i]);
		}
	    } else if (argv[i].equals("-j")) {
		schematronSC = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronSCXform = new File(argv[++i]);
		}
	    } else if (argv[i].equals("-k")) {
		schematronResults = true;
		int next = i+1;
		if (next < argv.length && !argv[next].startsWith("-")) {
		    schematronResultsXform = new File(argv[++i]);
		}
	    } else if (argv[i].equals("-plugin")) {
		loadPlugin(argv[++i]);
	    } else if (argv[i].equals("-config")) {
		pluginConfig = new Properties();
		try {
		    pluginConfig.load(new FileInputStream(new File(argv[++i])));
		    pluginConfig.setProperty(IPluginContainer.PROP_CONFIGFILE, argv[i]);
		} catch (IOException e) {
		    Main.logException(e);
		    Main.print(Main.getMessage("ERROR_PLUGIN_CONFIG", e.getMessage()));
		    return false;
		}
	    } else if (i == (argv.length - 1)) {
		specifiedChecksum = argv[i];
	    } else {
		Main.print(Main.getMessage("WARNING_ARG", argv[i]));
	    }
	}
	if (container == null && inputFile == null) {
	    loadPlugin(DEFAULT_PLUGIN);
	}
	return validState();
    }

    boolean processPluginArguments() {
	try {
	    if (container != null) {
		if (pluginConfig == null) {
		    File config = new File(BASE_DIR, IPluginContainer.DEFAULT_FILE);
		    if (config.exists()) {
			pluginConfig = new Properties();
			pluginConfig.setProperty(IPluginContainer.PROP_CONFIGFILE, config.getCanonicalPath());
			pluginConfig.load(new FileInputStream(config));
		    }
		}
		container.configure(pluginConfig);
	    }
	    return true;
	} catch (Exception e) {
	    Main.logException(e);
	    Main.print(Main.getMessage("ERROR_PLUGIN_CONFIG", e.getMessage(), logFile));
	}
	return false;
    }

    // Private

    private boolean validState() {
	if (!defsFile.exists()) {
	    Main.print(Main.getMessage("ERROR_NOSUCHFILE", defsFile.toString()));
	    return false;
	}
	if (!computeChecksum && specifiedChecksum == null && validateChecksum) {
	    Main.print(Main.getMessage("ERROR_NOCHECKSUM"));
	    return false;
	}
	if (inputFile != null) {
	    if (inputFile.exists()) {
		return true;
	    } else {
		Main.print(Main.getMessage("ERROR_INPUTFILE", inputFile));
		return false;
	    }
	} else if (container == null) {
	    Main.print(Main.getMessage("ERROR_PLUGIN"));
	    return false;
	}
	return true;
    }

    private String[] getPluginArgs(String s) {
	if (s.startsWith("\"")) {
	    s = s.substring(1);
	}
	if (s.endsWith("\"")) {
	    s = s.substring(0, s.length()-1);
	}
	StringTokenizer tok = new StringTokenizer(s);
	String[] args = new String[tok.countTokens()];
	for (int i=0; tok.hasMoreTokens(); i++) {
	    args[i] = tok.nextToken();
	}
	return args;
    }

    private boolean loadPlugin(String name) {
	try {
	    File pluginRootDir = new File(BASE_DIR, "plugin");
	    File[] pluginDirs = pluginRootDir.listFiles();
	    for (int i=0; i < pluginDirs.length; i++) {
		Properties pluginProperties = new Properties();
		File propsFile = new File(pluginDirs[i], "plugin.properties");
		if (propsFile.exists()) {
		    pluginProperties.load(new FileInputStream(propsFile));
		    if (name.equals(pluginProperties.getProperty("name"))) {
			String classpath = pluginProperties.getProperty("classpath");
			if (classpath == null) {
			    Main.print(Main.getMessage("ERROR_PLUGIN_CLASSPATH"));
			    return false;
			} else {
			    Vector<URL> vUrl = new Vector<URL>();
			    StringTokenizer tok = new StringTokenizer(classpath, ":");
			    while(tok.hasMoreTokens()) {
				String s = tok.nextToken();
				File f = new File(pluginDirs[i], s);
				if (f.exists()) {
				    vUrl.add(f.toURI().toURL());
				}
			    }
			    URL[] urls = vUrl.toArray(new URL[vUrl.size()]);
			    pluginClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
			    String main = pluginProperties.getProperty("main");
			    if (main == null) {
				Main.print(Main.getMessage("ERROR_PLUGIN_MAIN"));
				return false;
			    } else {
				Object pluginObject = pluginClassLoader.loadClass(main).newInstance();
				if (Class.forName(IPluginContainer.class.getName()).isInstance(pluginObject)) {
				    container = (IPluginContainer)pluginObject;
				    File dataDir = new File(pluginDirs[i], "data");
				    if (!dataDir.exists()) {
					dataDir.mkdirs();
				    }
				    container.setDataDirectory(dataDir);
				    return true;
				} else {
				    Main.print(Main.getMessage("ERROR_PLUGIN_IPLUGIN", main));
				    return false;
				}
			    }
			}
		    }
		}
	    }
	    Main.print(Main.getMessage("ERROR_PLUGIN_NOT_FOUND", name));
	    return false;
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	} catch (InstantiationException e) {
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return false;
    }
}
