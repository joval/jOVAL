// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.results.core.DefinitionType;

import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.oval.xml.SchematronValidationException;
import org.joval.oval.xml.SchematronValidator;
import org.joval.util.Checksum;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Command-Line Interface main class, whose purpose is to replicate the CLI of Ovaldi (the MITRE OVAL Definition
 * Interpreter).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Main implements IObserver {
    private static final String LF			= System.getProperty("line.separator");
    private static final String JAVA_VERSION		= System.getProperty("java.specification.version");
    private static final String MIN_JAVA_VERSION	= "1.6";

    private static ExecutionState state = null;
    private static String lastStatus = null;

    private static Logger logger = Logger.getLogger("jovaldi");

    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    LogManager.getLogManager().readConfiguration(cl.getResourceAsStream("jovaldi.logging.properties"));
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("jovaldi.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("jovaldi.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("jovaldi.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    /**
     * Definition Interpreter application entry-point.
     */
    public static void main (String[] argv) {
	if (new Version(JAVA_VERSION).compareTo(new Version(MIN_JAVA_VERSION)) < 0) {
	    print(getMessage("ERROR_JAVAVERSION", JAVA_VERSION, MIN_JAVA_VERSION));
	    System.exit(ERR);
	} else {
	    state = new ExecutionState();
	    if (state.processArguments(argv)) {
		Main main = new Main();
		printHeader();
		if (state.printHelp) {
		    printHelp();
		    System.exit(OK);
		} else if (state.processPluginArguments()) {
		    if (OK == main.exec()) {
			System.exit(OK);
		    } else {
			System.exit(ERR);
		    }
		} else {
		    printPluginHelp();
		    System.exit(ERR);
		}
	    } else {
		printHeader();
		printHelp();
		System.exit(ERR);
	    }
	}
    }

    // Internal

    /**
     * Print to both the log and the console.
     */
    static void print(String format, Object... args) {
	String s = String.format(format, args).toString();
	logger.log(Level.INFO, s);
	if (!state.printLogs) {
	    System.out.println(s);
	}
    }

    static void logException(Throwable thrown) {
	logger.log(Level.WARNING, getMessage("ERROR_FATAL"), thrown);
    }

    /**
     * Retrieve a message using its key.
     */
    static String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    // Private

    /**
     * Print to the console only (not the log).  Repeated calls to printStatus will all appear on a single line in the console,
     * over-writing the status message that was previously shown.  (Actually, it only over-writes the characters that differ
     * from the last status message, for a more fluid appearance when changes are very rapid).
     */
    private static void printStatus(String format, Object... args) {
	printStatus(format, false, args);
    }

    /**
     * @param clear set to true to proceed to the next line on the console.
     */
    private static void printStatus(String format, boolean clear, Object... args) {
	String s = String.format(format, args).toString();
	int offset=0;
	if (lastStatus != null) {
	    int len = lastStatus.length();
	    int n = Math.min(len, s.length());
	    for (int i=0; i < n; i++) {
		if (s.charAt(i) == lastStatus.charAt(i)) {
		    offset++;
		}
	    }
	    StringBuffer back = new StringBuffer();
	    StringBuffer clean = new StringBuffer();
	    int toClear = len - offset;
	    for (int i=0; i < toClear; i++) {
		back.append('\b');
		clean.append(' ');
	    }
	    System.out.print(back.toString());
	    System.out.print(clean.toString());
	    System.out.print(back.toString());
	}
	System.out.print(s.substring(offset));
	if (clear) {
	    System.out.println("");
	    lastStatus = null;
	} else {
	    lastStatus = s;
	}
    }

    /**
     * Print help text to the console.
     */
    private static void printHelp() {
	print("");
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("jovaldi.helptext_" + locale.toString());
	    if (url == null) {
		url = cl.getResource("jovaldi.helptext_" + locale.getLanguage());
	    }
	    if (url == null) {
		url = cl.getResource("jovaldi.helptext");
	    }
	    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		System.out.println(line);
	    }
	    printPluginHelp();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Print the plugin's help text to the console.
     */
    private static void printPluginHelp() {
	if (state.container != null) {
	    System.out.println(state.container.getProperty(IPluginContainer.PROP_HELPTEXT));
	}
    }

    /**
     * Print program information.
     */
    private static void printHeader() {
	print("");
	print(getMessage("MESSAGE_DIVIDER"));
	print(getMessage("MESSAGE_PRODUCT"));
	print(getMessage("MESSAGE_VERSION", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION)));
	print(getMessage("MESSAGE_BUILD_DATE", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_BUILD_DATE)));
	print(getMessage("MESSAGE_COPYRIGHT"));
	if (state.container != null) {
	    print("");
	    print(getMessage("MESSAGE_PLUGIN_NAME", state.container.getProperty(IPluginContainer.PROP_DESCRIPTION)));
	    print(getMessage("MESSAGE_PLUGIN_VERSION", state.container.getProperty(IPluginContainer.PROP_VERSION)));
	    print(getMessage("MESSAGE_PLUGIN_COPYRIGHT", state.container.getProperty(IPluginContainer.PROP_COPYRIGHT)));
	}
	print(getMessage("MESSAGE_DIVIDER"));
	print("");
	print(getMessage("MESSAGE_START_TIME", new Date()));
	print("");
    }

    // Implement IObserver

    public void notify(IProducer source, int msg, Object arg) {
	switch(msg) {
	  case IEngine.MESSAGE_OBJECT_PHASE_START:
	    print(getMessage("MESSAGE_OBJECT_PHASE"));
	    break;
	  case IEngine.MESSAGE_OBJECT:
	    printStatus(getMessage("MESSAGE_OBJECT", (String)arg));
	    break;
	  case IEngine.MESSAGE_OBJECT_PHASE_END:
	    printStatus(getMessage("MESSAGE_OBJECTS_DONE"), true);
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_START:
	    print(getMessage("MESSAGE_DEFINITION_PHASE"));
	    break;
	  case IEngine.MESSAGE_DEFINITION:
	    printStatus(getMessage("MESSAGE_DEFINITION", (String)arg));
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_END:
	    printStatus(getMessage("MESSAGE_DEFINITIONS_DONE"), true);
	    break;
	  case IEngine.MESSAGE_SYSTEMCHARACTERISTICS: {
	    ISystemCharacteristics sc = (ISystemCharacteristics)arg;
	    print(getMessage("MESSAGE_SAVING_SYSTEMCHARACTERISTICS", state.dataFile.toString()));
	    sc.writeXML(state.dataFile);
	    if (state.schematronSC) {
		try {
		    print(getMessage("MESSAGE_RUNNING_XMLVALIDATION", state.dataFile.toString()));
		    if (!validateSchema(state.dataFile, SYSTEMCHARACTERISTICS_SCHEMAS)) {
			state.getPlugin().disconnect();
			System.exit(ERR);
		    }
		    print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.dataFile.toString()));
		    OvalSystemCharacteristics osc = sc.getOvalSystemCharacteristics();
		    SchematronValidator.validate(osc, state.getSCSchematron());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (SchematronValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON_ERROR", i, errors.get(i)));
			    }
			}
		    }
		    state.getPlugin().disconnect();
		    System.exit(ERR);
		} catch (Exception e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }
	    break;
	  }
	}
    }

    // Private

    static String[] DEFINITIONS_SCHEMAS			= {"aix-definitions-schema.xsd",
							   "apache-definitions-schema.xsd",
							   "catos-definitions-schema.xsd",
							   "esx-definitions-schema.xsd",
							   "freebsd-definitions-schema.xsd",
							   "hpux-definitions-schema.xsd",
							   "independent-definitions-schema.xsd",
							   "ios-definitions-schema.xsd",
							   "linux-definitions-schema.xsd",
							   "macos-definitions-schema.xsd",
							   "oval-common-schema.xsd",
							   "oval-definitions-schema.xsd",
							   "pixos-definitions-schema.xsd",
							   "sharepoint-definitions-schema.xsd",
							   "solaris-definitions-schema.xsd",
							   "unix-definitions-schema.xsd",
							   "windows-definitions-schema.xsd",
							   "xmldsig-core-schema.xsd"};

    static String[] SYSTEMCHARACTERISTICS_SCHEMAS	= {"aix-system-characteristics-schema.xsd",
							   "apache-system-characteristics-schema.xsd",
							   "catos-system-characteristics-schema.xsd",
							   "esx-system-characteristics-schema.xsd",
							   "freebsd-system-characteristics-schema.xsd",
							   "hpux-system-characteristics-schema.xsd",
							   "independent-system-characteristics-schema.xsd",
							   "ios-system-characteristics-schema.xsd",
							   "linux-system-characteristics-schema.xsd",
							   "macos-system-characteristics-schema.xsd",
							   "oval-common-schema.xsd",
							   "oval-system-characteristics-schema.xsd",
							   "pixos-system-characteristics-schema.xsd",
							   "sharepoint-system-characteristics-schema.xsd",
							   "solaris-system-characteristics-schema.xsd",
							   "unix-system-characteristics-schema.xsd",
							   "windows-system-characteristics-schema.xsd",
							   "xmldsig-core-schema.xsd"};

    private static int OK	= 0;
    private static int ERR	= 1;

    private Main() {
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    LogManager.getLogManager().readConfiguration(cl.getResourceAsStream("jovaldi.logging.properties"));

	    Logger jSysLogger = Logger.getLogger(JOVALSystem.getLogger().getName());
	    Handler logHandler = new FileHandler(state.logFile.toString(), false);
	    logHandler.setFormatter(new LogfileFormatter());
	    logHandler.setLevel(state.logLevel);
	    logger.setLevel(state.logLevel);
	    logger.addHandler(logHandler);
	    jSysLogger.setLevel(state.logLevel);
	    jSysLogger.addHandler(logHandler);
	    if (state.printLogs) {
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(new ConsoleFormatter());
		consoleHandler.setLevel(state.logLevel);
		logger.addHandler(consoleHandler);
		jSysLogger.addHandler(consoleHandler);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Execute.
     */
    private int exec() {
	try {
	    if (state.computeChecksum) {
		print("");
		print(Checksum.getMD5Checksum(state.defsFile));
		return OK;
	    } else if (state.validateChecksum) {
		print(" ** verifying the MD5 hash of '" + state.defsFile.toString() + "' file");
		String checksum = Checksum.getMD5Checksum(state.defsFile);
		if (!state.specifiedChecksum.equals(checksum)) {
		    print(getMessage("ERROR_CHECKSUM_MISMATCH", state.defsFile.toString()));
		    return ERR;
		}
	    }

	    print(getMessage("MESSAGE_PARSING_FILE", state.defsFile.toString()));
	    IDefinitions defs = JOVALSystem.createDefinitions(state.defsFile);

	    print(getMessage("MESSAGE_VALIDATING_XML"));
	    if (!validateSchema(state.defsFile, DEFINITIONS_SCHEMAS)) {
		return ERR;
	    }

	    print(getMessage("MESSAGE_SCHEMA_VERSION_CHECK"));
	    Version schemaVersion = new Version(defs.getOvalDefinitions().getGenerator().getSchemaVersion());
	    print(getMessage("MESSAGE_SCHEMA_VERSION", schemaVersion.toString()));
	    if (IEngine.SCHEMA_VERSION.compareTo(schemaVersion) < 0) {
		print(getMessage("ERROR_SCHEMA_VERSION", schemaVersion.toString()));
		return ERR;
	    }

	    if (state.schematronDefs) {
		print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.defsFile.toString()));
		try {
		    SchematronValidator.validate(defs.getOvalDefinitions(), state.getDefsSchematron());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (SchematronValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON", errors.get(i)));
			    }
			}
		    }
		    return ERR;
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }

	    IEngine engine = JOVALSystem.createEngine(state.getPlugin());
	    engine.setDefinitions(defs);
	    if (state.inputFile == null) {
		print(getMessage("MESSAGE_CREATING_SYSTEMCHARACTERISTICS"));
	    } else {
		print(" ** parsing " + state.inputFile.toString() + " for analysis.");
		print(getMessage("MESSAGE_VALIDATING_XML"));
		if (validateSchema(state.inputFile, SYSTEMCHARACTERISTICS_SCHEMAS)) {
		    engine.setSystemCharacteristicsFile(state.inputFile);
		} else {
		    return ERR;
		}
	    }
	    if (state.variablesFile.exists() && state.variablesFile.isFile()) {
		engine.setExternalVariablesFile(state.variablesFile);
	    }
	    if (state.inputDefsFile != null) {
		print(getMessage("MESSAGE_READING_INPUTDEFINITIONS", state.inputDefsFile));
		engine.setDefinitionFilter(JOVALSystem.createDefinitionFilter(state.inputDefsFile));
	    } else if (state.definitionIDs != null) {
		print(getMessage("MESSAGE_PARSING_INPUTDEFINITIONS"));
		engine.setDefinitionFilter(JOVALSystem.createAcceptFilter(state.definitionIDs));
	    }
	    engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
	    engine.run();
	    switch(engine.getResult()) {
	      case ERR:
		throw engine.getError();
	    }

	    IResults results = engine.getResults();
	    if (state.directivesFile.exists() && state.directivesFile.isFile()) {
		print(getMessage("MESSAGE_APPLYING_DIRECTIVES"));
		results.setDirectives(state.directivesFile);
	    }
	    print(getMessage("MESSAGE_RESULTS"));
	    print("");
	    print(getMessage("MESSAGE_DEFINITION_TABLE_HEAD"));
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));

	    for (DefinitionType d : results.getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()) {
		String id = d.getDefinitionId();
		String result = d.getResult().toString().toLowerCase();
		print(getMessage("MESSAGE_DEFINITION_TABLE_ROW", String.format("%-40s", id), result));
	    }
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));
	    print("");
	    print("");
	    print(getMessage("MESSAGE_DEFINITIONS_EVALUATED"));
	    print("");
	    print(getMessage("MESSAGE_SAVING_RESULTS", state.resultsXML.toString()));
	    results.writeXML(state.resultsXML);
	    if (state.schematronResults) {
		print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.resultsXML.toString()));
		try {
		    SchematronValidator.validate(defs.getOvalDefinitions(), state.getResultsSchematron());
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} catch (SchematronValidationException e) {
		    List<String> errors = e.getErrors();
		    if (errors == null) {
			print(e.getMessage());
			logger.log(Level.SEVERE, e.getMessage(), e);
		    } else {
			for (int i=0; i < errors.size(); i++) {
			    if (i == 0) {
				print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			    } else {
				logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON", errors.get(i)));
			    }
			}
		    }
		    return ERR;
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }
	    if (state.applyTransform) {
		print(getMessage("MESSAGE_RUNNING_TRANSFORM", state.getXMLTransform().toString()));
		results.writeTransform(state.getXMLTransform(), state.resultsTransform);
	    } else {
		print(getMessage("MESSAGE_SKIPPING_TRANSFORM"));
	    }
	    print("");
	    print(getMessage("MESSAGE_DIVIDER"));
	    return OK;
	} catch (OvalException e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_OVAL"));
	    print("");
	    e.printStackTrace();
	    return ERR;
	} catch (Exception e) {
	    print("");
	    print("");
	    print(getMessage("ERROR_FATAL"));
	    e.printStackTrace();
	    return ERR;
	}
    }

    private boolean validateSchema(File f, String[] fnames) throws SAXException, IOException {
	ArrayList<Source> list = new ArrayList<Source>();
	for (int i=0; i < fnames.length; i++) {
	    File schemaFile = new File(state.xmlDir, fnames[i]);
	    if (schemaFile.exists()) {
		list.add(new StreamSource(schemaFile));
	    }
	}
	Source[] sources = new Source[0];
	Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(list.toArray(sources));
	Validator validator = schema.newValidator();
	try {
	    validator.validate(new StreamSource(f));
	    return true;
	} catch (SAXException e) {
	    print(getMessage("ERROR_VALIDATION", e.getMessage()));
	    return false;
	}
    }

    private class ConsoleFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(record.getMessage());
	    line.append(LF);
	    return line.toString();
	}
    }

    private class LogfileFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(record.getMessage());
	    line.append(LF);
	    Throwable thrown = record.getThrown();
	    if (thrown != null) {
		line.append(thrown.toString());
		line.append(LF);
		StackTraceElement[] ste = thrown.getStackTrace();
		for (int i=0; i < ste.length; i++) {
		    line.append("    at ");
		    line.append(ste[i].getClassName());
		    line.append(".");
		    line.append(ste[i].getMethodName());
		    line.append(", ");
		    line.append(ste[i].getFileName());
		    line.append(" line: ");
		    line.append(Integer.toString(ste[i].getLineNumber()));
		    line.append(LF);
		}
	    }
	    return line.toString();
	}
    }
}
