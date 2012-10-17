// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joval.intf.plugin.IPlugin;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.Datastream;
import org.joval.scap.ScapException;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.Profile;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xml.SignatureValidator;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT {
    private static File CWD = new File(".");
    private static File BASE_DIR = CWD;
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
	boolean help = false;
	File streamFile = new File(CWD, "scap-datastream.xml");
	String streamId = null;
	String benchmarkId = null;
	String profileName = null;
	Hashtable<String, File> ocilFiles = new Hashtable<String, File>();
	File resultsFile = new File(CWD, "xccdf-results.xml");
	File xmlDir = new File(BASE_DIR, "xml");
	File transformFile = new File(xmlDir, "xccdf_results_to_html.xsl");
	File reportFile = new File("xccdf-result.html");
	File ocilDir = new File("ocil-export");

	File ovalDir = null;
	Level level = Level.INFO;
	boolean query = false, verify = false;
	File logFile = new File(CWD, "xpert.log");
	String pluginName = "default";
	File configFile = new File(CWD, IPlugin.DEFAULT_FILE);

	for (int i=0; i < argv.length; i++) {
	    if (argv[i].equals("-h")) {
		help = true;
	    } else if (argv[i].equals("-q")) {
		query = true;
	    } else if (argv[i].equals("-s")) {
		verify = true;
	    } else if ((i + 1) < argv.length) {
		if (argv[i].equals("-d")) {
		    streamFile = new File(argv[++i]);
		} else if (argv[i].equals("-s")) {
		    streamId = argv[++i];
		} else if (argv[i].equals("-b")) {
		    benchmarkId = argv[++i];
		} else if (argv[i].equals("-p")) {
		    profileName = argv[++i];
		} else if (argv[i].equals("-i")) {
		    String pair = argv[++i];
		    int ptr = pair.indexOf("=");
		    String key, val;
		    if (ptr == -1) {
			key = "";
			val = pair;
		    } else {
			key = pair.substring(0,ptr);
			val = pair.substring(ptr+1);
		    }
		    if (ocilFiles.containsKey(key)) {
			System.out.println("WARNING: duplicate OCIL href - " + key);
		    }
		    ocilFiles.put(key, new File(val));
		} else if (argv[i].equals("-r")) {
		    resultsFile = new File(argv[++i]);
		} else if (argv[i].equals("-v")) {
		    ovalDir = new File(argv[++i]);
		} else if (argv[i].equals("-l")) {
		    try {
			switch(Integer.parseInt(argv[++i])) {
			  case 4:
			    level = Level.SEVERE;
			    break;
			  case 3:
			    level = Level.WARNING;
			    break;
			  case 2:
			    level = Level.INFO;
			    break;
			  case 1:
			    level = Level.FINEST;
			    break;
			  default:
			    System.out.println("WARNING log level value not in range: " + argv[i]);
			    break;
			}
		    } catch (NumberFormatException e) {
			System.out.println("WARNING illegal log level value: " + argv[i]);
		    }
		} else if (argv[i].equals("-y")) {
		    logFile = new File(argv[++i]);
		} else if (argv[i].equals("-e")) {
		    ocilDir = new File(argv[++i]);
		} else if (argv[i].equals("-t")) {
		    transformFile = new File(argv[++i]);
		} else if (argv[i].equals("-x")) {
		    reportFile = new File(argv[++i]);
		} else if (argv[i].equals("-plugin")) {
		    pluginName = argv[++i];
		} else if (argv[i].equals("-config")) {
		    configFile = new File(argv[++i]);
		} else {
		    System.out.println("WARNING unrecognized command-line argument: " + argv[i]);
		}
	    } else {
		System.out.println("WARNING unrecognized command-line argument: " + argv[i]);
	    }
	}

	int exitCode = 1;
	if (ovalDir != null && !ovalDir.exists()) {
	    ovalDir.mkdir();
	}
	try {
	    logger = LogFormatter.createDuplex(logFile, level);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

	IPlugin plugin = null;
	try {
	    File configDir = new File(BASE_DIR, "config");
	    if (configDir.isDirectory()) {
		File configOverrides = new File(configDir, "xpert.ini");
		if (configOverrides.isFile()) {
		    JOVALSystem.addConfiguration(configOverrides);
		}
	    }
	    plugin = PluginFactory.newInstance(new File(BASE_DIR, "plugin")).createPlugin(pluginName);
	} catch (IOException e) {
	    logger.severe(LogFormatter.toString(e));
	} catch (IllegalArgumentException e) {
	    logger.severe("Not a directory: " + e.getMessage());
	} catch (NoSuchElementException e) {
	    logger.severe("Plugin not found: " + e.getMessage());
	} catch (PluginConfigurationException e) {
	    logger.severe(LogFormatter.toString(e));
	}

	printHeader(plugin);
	if (help) {
	    printHelp(plugin);
	    exitCode = 0;
	} else if (!streamFile.isFile()) {
	    logger.warning("ERROR: No such file " + streamFile.toString());
	    printHelp(plugin);
	} else {
	    try {
		Datastream ds = null;
		if (verify) {
		    logger.info("Verifying XML digital signature: " + streamFile.toString());
		    SignatureValidator validator = new SignatureValidator(streamFile);
		    if (!validator.containsSignature()) {
			throw new XPERTException("ERROR: no signature found!");
		    } else if (validator.validate()) {
			logger.info("Signature validated");
			logger.info("Loading Data Stream...");
			ds = new Datastream(validator.getSource());
		    } else {
			throw new XPERTException("ERROR: signature validation failed!");
		    }
		} else {
		    logger.info("Loading Data Stream: " + streamFile.toString());
		    ds = new Datastream(streamFile);
		}

		if (query) {
		    logger.info("Querying Data Stream: " + streamFile.toString());
		    for (String sId : ds.getStreamIds()) {
			logger.info("Stream ID=\"" + sId + "\"");
			for (String bId : ds.getBenchmarkIds(sId)) {
			    logger.info("  Benchmark ID=\"" + bId + "\"");
			    for (String profileId : ds.getBenchmark(sId, bId).getProfileIds()) {
				logger.info("    Profile Name=\"" + profileId + "\"");
			    }
			}
		    }
		} else if (plugin == null) {
		    printHelp(null);
		} else {
		    logger.info("Start time: " + new Date().toString());
		    try {
			//
			// Configure the jOVAL plugin
			//
			Properties config = new Properties();
			if (configFile.isFile()) {
			    config.load(new FileInputStream(configFile));
			}
			plugin.configure(config);
		    } catch (Exception e) {
			throw new XPERTException("Problem configuring the plugin:\n  " + e.getMessage());
		    }

		    if (streamId == null) {
			if (ds.getStreamIds().size() == 1) {
			    streamId = ds.getStreamIds().iterator().next();
			    logger.info("Selected stream " + streamId);
			} else {
			    throw new XPERTException("ERROR: A stream must be selected for this stream collection source");
			}
		    }

		    if (benchmarkId == null) {
			if (ds.getBenchmarkIds(streamId).size() == 1) {
			    benchmarkId = ds.getBenchmarkIds(streamId).iterator().next();
			    logger.info("Selected benchmark " + benchmarkId);
			} else {
			    throw new XPERTException("ERROR: A benchmark must be selected for stream " + streamId);
			}
		    }

		    Hashtable<String, Checklist> checklists = new Hashtable<String, Checklist>();
		    for (String href : ocilFiles.keySet()) {
			try {
			    checklists.put(href, new Checklist(ocilFiles.get(href)));
			} catch (OcilException e) {
			    logger.severe(e.getMessage());
			    System.exit(1);
			}
		    }

		    try {
			Benchmark benchmark = ds.getBenchmark(streamId, benchmarkId);
			Profile profile = new Profile(benchmark, profileName);
			Engine engine = new Engine(benchmark, profile, checklists, ocilDir, plugin.getSession(), ovalDir);
			engine.run();

			if (benchmark.getBenchmark().isSetTestResult()) {
			    logger.info("Saving report: " + resultsFile.toString());
			    benchmark.writeBenchmarkXML(resultsFile);
			    logger.info("Transforming to HTML report: " + reportFile.toString());
			    benchmark.writeTransform(transformFile, reportFile);
			}

			logger.info("Finished processing XCCDF bundle");
			exitCode = 0;
		    } catch (UnknownHostException e) {
			logger.severe(">>> ERROR - No such host: " + e.getMessage());
		    } catch (ConnectException e) {
			logger.severe(">>> ERROR - Failed to connect to host: " + e.getMessage());
		    }
		}
	    } catch (ScapException e) {
		logger.warning(LogFormatter.toString(e));
		exitCode = 2;
	    } catch (XPERTException e) {
		logger.severe(e.getMessage());
		exitCode = 1;
	    } catch (Exception e) {
		logger.severe(LogFormatter.toString(e));
		exitCode = 3;
	    }
	}
	System.exit(exitCode);
    }

    // Internal

    static class XPERTException extends Exception {
	XPERTException(String message) {
	    super(message);
	}
    }
}
