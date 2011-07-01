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
import oval.schemas.results.core.DefinitionType;

import org.joval.intf.di.IJovaldiPlugin;
import org.joval.intf.oval.IResults;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.oval.engine.DefinitionFilter;
import org.joval.oval.engine.Engine;
import org.joval.oval.engine.SystemCharacteristics;
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

    private static Logger logger = JOVALSystem.getLogger();
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
	if (new Version(MIN_JAVA_VERSION).greaterThan(new Version(JAVA_VERSION))) {
	    print(getMessage("ERROR_JAVAVERSION", JAVA_VERSION, MIN_JAVA_VERSION));
	    System.exit(ERR);
	} else {
	    state = new ExecutionState();
	    if (state.processArguments(argv)) {
		Main main = new Main();
		printHeader();
		if (state.processPluginArguments()) {
		    if (OK == main.exec()) {
			System.exit(OK);
		    } else {
			System.exit(ERR);
		    }
		} else {
		    print(getMessage("ERROR_PLUGIN_CONFIG", state.getPluginError()));
		    printPluginHelp();
		    System.exit(ERR);
		}
	    } else {
		printHeader();
		printHelp();
		System.exit(1);
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
		print(line);
	    }
	    printPluginHelp();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static void printPluginHelp() {
	if (state.plugin != null) {
	    print(state.plugin.getProperty(IJovaldiPlugin.PROP_HELPTEXT));
	}
    }

    /**
     * Print program information.
     */
    private static void printHeader() {
	print("");
	print(getMessage("MESSAGE_DIVIDER"));
	print(getMessage("MESSAGE_PRODUCT"));
	print(getMessage("MESSAGE_VERSION", JOVALSystem.getProperty(JOVALSystem.PROP_VERSION)));
	print(getMessage("MESSAGE_BUILD_DATE", JOVALSystem.getProperty(JOVALSystem.PROP_BUILD_DATE)));
	print(getMessage("MESSAGE_COPYRIGHT"));
	print("");
	print(getMessage("MESSAGE_PLUGIN_NAME", state.plugin.getProperty(IJovaldiPlugin.PROP_DESCRIPTION)));
	print(getMessage("MESSAGE_PLUGIN_VERSION", state.plugin.getProperty(IJovaldiPlugin.PROP_VERSION)));
	print(getMessage("MESSAGE_PLUGIN_COPYRIGHT", state.plugin.getProperty(IJovaldiPlugin.PROP_COPYRIGHT)));
	print(getMessage("MESSAGE_DIVIDER"));
	print("");
	print(getMessage("MESSAGE_START_TIME", new Date()));
	print("");
    }

    // Implement IObserver

    public void notify(IProducer source, int msg, Object arg) {
	switch(msg) {
	  case Engine.MESSAGE_OBJECT_PHASE_START:
	    print(getMessage("MESSAGE_OBJECT_PHASE"));
	    break;
	  case Engine.MESSAGE_OBJECT:
	    printStatus(getMessage("MESSAGE_OBJECT", (String)arg));
	    break;
	  case Engine.MESSAGE_OBJECT_PHASE_END:
	    printStatus(getMessage("MESSAGE_OBJECTS_DONE"), true);
	    break;
	  case Engine.MESSAGE_DEFINITION_PHASE_START:
	    print(getMessage("MESSAGE_DEFINITION_PHASE"));
	    break;
	  case Engine.MESSAGE_DEFINITION:
	    printStatus(getMessage("MESSAGE_DEFINITION", (String)arg));
	    break;
	  case Engine.MESSAGE_DEFINITION_PHASE_END:
	    printStatus(getMessage("MESSAGE_DEFINITIONS_DONE"), true);
	    break;
	  case Engine.MESSAGE_SYSTEMCHARACTERISTICS:
	    print(getMessage("MESSAGE_SAVING_SYSTEMCHARACTERISTICS", (String)arg));
	    break;
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

    private boolean noNamespaces = false;

    private Main() {
	try {
	    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("jovaldi.logging.properties");
	    LogManager.getLogManager().readConfiguration(in);
	    JOVALSystem.getLogger().setLevel(state.logLevel);
	    Handler logHandler = new FileHandler(state.logFile.toString(), false);
	    logHandler.setFormatter(new LogfileFormatter());
	    JOVALSystem.getLogger().addHandler(logHandler);
	    if (state.printLogs) {
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(new ConsoleFormatter());
		consoleHandler.setLevel(state.logLevel);
		JOVALSystem.getLogger().addHandler(consoleHandler);
	    } else {
//		consoleHandler.setLevel(Level.SEVERE);
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
	    if (state.printHelp) {
		printHelp();
		return OK;
	    } else if (state.computeChecksum) {
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
	    if (!validateSchema(state.defsFile, DEFINITIONS_SCHEMAS)) {
		return ERR;
	    }

	    OvalDefinitions defs = Engine.getOvalDefinitions(state.defsFile);
	    print(getMessage("MESSAGE_SCHEMA_VERSION_CHECK"));
	    BigDecimal schemaVersion = defs.getGenerator().getSchemaVersion();
	    print(getMessage("MESSAGE_SCHEMA_VERSION", schemaVersion.toString()));
	    if (schemaVersion.compareTo(Engine.SCHEMA_VERSION) > 0) { // must be <= engine's version
		print(getMessage("ERROR_SCHEMA_VERSION", schemaVersion.toString()));
		return ERR;
	    }

	    if (state.applySchematron) {
		print(getMessage("MESSAGE_RUNNING_SCHEMATRON", state.defsFile.toString()));
		if (Engine.schematronValidate(defs, state.getSchematronTransform())) {
		    print(getMessage("MESSAGE_SCHEMATRON_SUCCESS"));
		} else {
		    List<String> errors = Engine.getSchematronValidationErrors();
		    for (int i=0; i < errors.size(); i++) {
			if (i == 0) {
			    print(getMessage("ERROR_SCHEMATRON", new Integer(errors.size()), errors.get(i)));
			} else {
			    logger.log(Level.SEVERE, getMessage("ERROR_SCHEMATRON", errors.get(i)));
			}
		    }
		    return ERR;
		}
	    } else {
		print(getMessage("MESSAGE_SKIPPING_SCHEMATRON"));
	    }

	    state.plugin.connect();
	    Engine engine = null;
	    if (state.inputFile == null) {
		engine = new Engine(defs, state.plugin);
		print(getMessage("MESSAGE_CREATING_SYSTEMCHARACTERISTICS"));
		engine.setSystemCharacteristicsOutputFile(state.dataFile, noNamespaces);
	    } else {
		print(" ** parsing " + state.inputFile.toString() + " for analysis.");
		if (!validateSchema(state.inputFile, SYSTEMCHARACTERISTICS_SCHEMAS)) {
		    return ERR;
		}
		SystemCharacteristics sc = new SystemCharacteristics(state.inputFile);
		engine = new Engine(defs, state.plugin, sc);
	    }
	    if (state.variablesFile.exists() && state.variablesFile.isFile()) {
		engine.setExternalVariablesFile(state.variablesFile);
	    }
	    if (state.inputDefsFile != null) {
		print(getMessage("MESSAGE_READING_INPUTDEFINITIONS", state.inputDefsFile));
		engine.setDefinitionFilter(new DefinitionFilter(state.inputDefsFile));
	    } else if (state.definitionIDs != null) {
		print(getMessage("MESSAGE_PARSING_INPUTDEFINITIONS"));
		engine.setDefinitionFilter(new DefinitionFilter(state.definitionIDs));
	    }
	    engine.addObserver(this, Engine.MESSAGE_MIN, Engine.MESSAGE_MAX);
	    engine.run();
	    state.plugin.disconnect();

	    IResults results = engine.getResults();
	    if (state.directivesFile.exists() && state.directivesFile.isFile()) {
		print(getMessage("MESSAGE_APPLYING_DIRECTIVES"));
		results.setDirectives(state.directivesFile);
	    }
	    print(getMessage("MESSAGE_RESULTS"));
	    print("");
	    print(getMessage("MESSAGE_DEFINITION_TABLE_HEAD"));
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));
	    Iterator<DefinitionType> definitionIter = results.iterateDefinitions();
	    while(definitionIter.hasNext()) {
		DefinitionType definition = definitionIter.next();
		String id = definition.getDefinitionId();
		String result = definition.getResult().toString().toLowerCase();
		print(getMessage("MESSAGE_DEFINITION_TABLE_ROW", String.format("%-40s", id), result));
	    }
	    print(getMessage("MESSAGE_DEFINITION_TABLE_DIV"));
	    print("");
	    print("");
	    print(getMessage("MESSAGE_DEFINITIONS_EVALUATED"));
	    print("");
	    print(getMessage("MESSAGE_SAVING_RESULTS", state.resultsXML.toString()));
	    results.writeXML(state.resultsXML, noNamespaces);
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
	    e.printStackTrace();
	    return ERR;
	} catch (Exception e) {
	    e.printStackTrace();
	    return ERR;
	}
    }

    private boolean validateSchema(File f, String[] fnames) throws SAXException, IOException {
	print(getMessage("MESSAGE_VALIDATING_XML"));
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
