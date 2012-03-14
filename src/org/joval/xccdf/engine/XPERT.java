// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import cpe.schemas.dictionary.ListType;
import oval.schemas.common.GeneratorType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.DefinitionType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.variables.core.VariableType;

import xccdf.schemas.core.Benchmark;
import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.ProfileType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.TestResultType;

import org.joval.cpe.CpeException;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.plugin.IPluginContainer;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.DefinitionFilter;
import org.joval.oval.OvalException;
import org.joval.oval.Variables;
import org.joval.plugin.container.ContainerFactory;
import org.joval.plugin.container.ContainerConfigurationException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.handler.OVALHandler;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT implements Runnable, IObserver {
    public static final String OVAL_SYSTEM	= "http://oval.mitre.org/XMLSchema/oval-definitions-5";
    public static final String SCE_SYSTEM	= "http://open-scap.org/page/SCE";

    private static final File ws = new File("artifacts");
    private static Logger logger;
    private static boolean debug = false;

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

    /**
     * Get the OVAL generator_type for the XPERT engine.
     */
    public static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.factories.common.createGeneratorType();
	generator.setProductName(getMessage("product.name"));
	generator.setProductVersion(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION));
	generator.setSchemaVersion(IEngine.SCHEMA_VERSION.toString());
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_TIMESTAMP);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return generator;
    }

    static void printHeader(IPluginContainer container) {
	PrintStream console = System.out;
	console.println(getMessage("divider"));
	console.println(getMessage("product.name"));
	console.println(getMessage("description"));
	console.println(getMessage("message.version", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION)));
	console.println(getMessage("message.buildDate", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_BUILD_DATE)));
	console.println(getMessage("copyright"));
	if (container != null) {
	    console.println("");
	    console.println(getMessage("message.plugin.name", container.getProperty(IPluginContainer.PROP_DESCRIPTION)));
	    console.println(getMessage("message.plugin.version", container.getProperty(IPluginContainer.PROP_VERSION)));
	    console.println(getMessage("message.plugin.copyright", container.getProperty(IPluginContainer.PROP_COPYRIGHT)));
	}
	console.println(getMessage("divider"));
	console.println("");
    }

    static void printHelp(IPluginContainer container) {
	System.out.println(getMessage("helpText"));
	if (container != null) {
	    System.out.println(container.getProperty(IPluginContainer.PROP_HELPTEXT));
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

	IPluginContainer container = null;
	try {
	    container = ContainerFactory.newInstance(new File(BASE_DIR, "plugin")).createContainer(pluginName);
	} catch (IllegalArgumentException e) {
	    logger.severe("Not a directory: " + e.getMessage());
	} catch (NoSuchElementException e) {
	    logger.severe("Plugin not found: " + e.getMessage());
	} catch (ContainerConfigurationException e) {
	    logger.severe(LogFormatter.toString(e));
	}

	printHeader(container);
	if (printHelp) {
	    printHelp(container);
	    exitCode = 0;
	} else if (xccdfBaseName == null) {
	    logger.warning("No XCCDF file was specified");
	    printHelp(container);
	} else if (container == null) {
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
		container.configure(config);

		//
		// Load the XCCDF and selected profile
		//
		logger.info("Loading " + xccdfBaseName);
		XccdfBundle xccdf = new XccdfBundle(new File(xccdfBaseName));
		Profile profile = new Profile(xccdf, profileName);

		//
		// Perform the evaluation
		//
		XPERT engine = new XPERT(xccdf, profile, container.getPlugin());
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
		logger.severe("Problem configuring the plugin -- check that the configuration is valid");
		logger.severe(LogFormatter.toString(e));
	    }
	}

	System.exit(exitCode);
    }

    private XccdfBundle xccdf;
    private IEngine engine;
    private Collection<String> platforms;
    private Profile profile;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;

    /**
     * Create an XCCDF Processing Engine and Report Tool using the specified XCCDF document bundle and jOVAL plugin.
     */
    public XPERT(XccdfBundle xccdf, Profile profile, IPlugin plugin) {
	this.xccdf = xccdf;
	this.profile = profile;
	engine = JOVALSystem.createEngine(plugin);
	engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
    }

    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
	if (hasSelection() && isApplicable()) {
	    phase = "evaluation";
	    OVALHandler ovalHandler = new OVALHandler(xccdf, profile);

	    //
	    // Configure the OVAL engine...
	    //
	    engine.setDefinitions(xccdf.getOval());
	    engine.setDefinitionFilter(ovalHandler.getDefinitionFilter());
	    engine.setExternalVariables(ovalHandler.getVariables());

	    //
	    // Read previously collected system characteristics data, if available.
	    //
	    if (debug) {
		try {
		    File sc = new File(ws, "sc-input.xml");
		    if(sc.isFile()) {
			logger.info("Loading " + sc);
			engine.setSystemCharacteristicsFile(sc);
		    }
		} catch (OvalException e) {
		    logger.warning(e.getMessage());
		}
	    }

	    //
	    // Run the OVAL engine
	    //
	    engine.run();
	    IResults results = null;
	    switch(engine.getResult()) {
	      case ERR:
		logger.severe(LogFormatter.toString(engine.getError()));
		return;
	      case OK:
		results = engine.getResults();
		if (debug) {
		    File resultsFile = new File(ws, "oval-results.xml");
		    logger.info("Saving OVAL results: " + resultsFile.getPath());
		    results.writeXML(resultsFile);
		}
		break;
	    }

	    //
	    // Create the Benchmark.TestResult node
	    //
	    ObjectFactory factory = new ObjectFactory();
	    TestResultType testResult = factory.createTestResultType();
	    testResult.setTestSystem(getMessage("product.name"));

	    ovalHandler.integrateResults(results, testResult);

	    Hashtable<String, RuleResultType> resultIndex = new Hashtable<String, RuleResultType>();
	    for (RuleResultType rrt : testResult.getRuleResult()) {
		resultIndex.put(rrt.getIdref(), rrt);
	    }

	    for (RuleType rule : listAllRules()) {
		String ruleId = rule.getItemId();
		if (resultIndex.containsKey(ruleId)) {
		    logger.info(ruleId + ": " + resultIndex.get(ruleId).getResult());
		} else {
		    //
		    // Add the unchecked result, just for fun.
		    //
		    RuleResultType rrt = factory.createRuleResultType();
		    rrt.setResult(ResultEnumType.NOTCHECKED);
		    if (rule.isSetCheck()) {
			for (CheckType check : rule.getCheck()) {
			    rrt.getCheck().add(check);
			}
		    }
		    testResult.getRuleResult().add(rrt);
		}
	    }

	    xccdf.getBenchmark().getTestResult().add(testResult);
	    File reportFile = new File(ws, "xccdf-results.xml");
	    logger.info("Saving report: " + reportFile.getPath());
	    xccdf.writeBenchmarkXML(reportFile);
	}
	logger.info("XCCDF processing complete.");
    }

    // Implement IObserver

    public void notify(IProducer sender, int msg, Object arg) {
	switch(msg) {
	  case IEngine.MESSAGE_OBJECT_PHASE_START:
	    logger.info("Beginning scan");
	    break;
	  case IEngine.MESSAGE_OBJECT:
	    logger.info("Scanning object " + (String)arg);
	    break;
	  case IEngine.MESSAGE_OBJECT_PHASE_END:
	    logger.info("Scan complete");
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_START:
	    logger.info("Evaluating definitions");
	    break;
	  case IEngine.MESSAGE_DEFINITION:
	    logger.info("Evaluating " + (String)arg);
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_END:
	    logger.info("Completed evaluating definitions");
	    break;
	  case IEngine.MESSAGE_SYSTEMCHARACTERISTICS:
	    if (debug) {
		File scFile = new File(ws, "sc-" + phase + ".xml");
		logger.info("Saving OVAL system-characteristics: " + scFile.getPath());
		((ISystemCharacteristics)arg).writeXML(scFile);
	    }
	    break;
	}
    }

    // Private

    /**
     * Check whether or not the Profile has any selected rules.
     */
    private boolean hasSelection() {
	Collection<RuleType> rules = profile.getSelectedRules();
	if (rules.size() == 0) {
	    logger.severe("No reason to evaluate!");
	    List<ProfileType> profiles = xccdf.getBenchmark().getProfile();
	    if (profiles.size() > 0) {
		logger.info("Try selecting a profile:");
		for (ProfileType pt : profiles) {
		    logger.info("  " + pt.getProfileId());
		}
	    }
	    return false;
	} else {
	    logger.info("There are " + rules.size() + " rules to process for the selected profile");
	    return true;
	}
    }

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied.
     */
    private boolean isApplicable() {
	phase = "discovery";
	logger.info("Determining system applicability...");

	//
	// Create a DefinitionFilter containing all the OVAL definitions corresponding to the CPE platforms.
	//
	Collection<String> definitions = profile.getPlatformDefinitionIds();
	if (definitions.size() == 0) {
	    logger.info("No platforms specified, skipping applicability checks...");
	    return true;
	}
	DefinitionFilter filter = new DefinitionFilter();
	for (String definition : definitions) {
	    filter.addDefinition(definition);
	}

	//
	// Evaluate the platform definitions.
	//
	engine.setDefinitionFilter(filter);
	engine.setDefinitions(xccdf.getCpeOval());
	engine.run();
	for (DefinitionType def :
	    engine.getResults().getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()) {
	    if (filter.accept(def.getDefinitionId())) {
		switch(def.getResult()) {
		  case TRUE:
		    logger.info("Passed def " + def.getDefinitionId());
		    break;
		  default:
		    logger.warning("Bad result " + def.getResult() + " for definition " + def.getDefinitionId());
		    logger.info("The target system is not applicable to the XCCDF bundle");
		    return false;
		}
	    } else {
		logger.info("Ignoring definition " + def.getDefinitionId());
	    }
	}
	logger.info("The target system is applicable to the XCCDF bundle");
	return true;
    }

    /**
     * Create an OVAL DefinitionFilter containing every selected rule.
     */
    private DefinitionFilter createFilter(Collection<RuleType> rules) {
	DefinitionFilter filter = new DefinitionFilter();
	for (RuleType rule : rules) {
	    if (rule.isSetCheck()) {
		logger.info("Getting checks for Rule " + rule.getItemId());
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(OVAL_SYSTEM)) {
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetName()) {
				logger.info("Adding check " + ref.getName());
				filter.addDefinition(ref.getName());
			    }
			}
		    }
		}
	    } else {
		logger.info("No check in Rule " + rule.getItemId());
	    }
	}
	if (debug) {
	    File filterFile = new File(ws, "oval-filter.xml");
	    logger.info("Saving OVAL evaluation-definitions: " + filterFile.getPath());
	    filter.writeXML(filterFile);
	}
	return filter;
    }

    /**
     * Gather all the variable exports from the rules, and create an OVAL variables structure containing their values.
     */
    private Variables createVariables(Collection<RuleType> rules, Hashtable<String, String> values) {
	Variables variables = new Variables(getGenerator());
	for (RuleType rule : rules) {
	    for (CheckType check : rule.getCheck()) {
		if (check.getSystem().equals(OVAL_SYSTEM)) {
		    for (CheckExportType export : check.getCheckExport()) {
			String ovalVariableId = export.getExportName();
			String valueId = export.getValueId();
			variables.addValue(ovalVariableId, values.get(valueId));
			variables.setComment(ovalVariableId, valueId);
		    }
		}
	    }
	}
	if (debug) {
	    File variablesFile = new File(ws, "oval-variables.xml");
	    logger.info("Saving OVAL variables: " + variablesFile.getPath());
	    variables.writeXML(variablesFile);
	}
	return variables;
    }

    /**
     * Recursively get all rules within the XCCDF document.
     */
    private List<RuleType> listAllRules() {
	List<RuleType> rules = new Vector<RuleType>();
	for (SelectableItemType item : xccdf.getBenchmark().getGroupOrRule()) {
	    rules.addAll(getRules(item));
	}
	return rules;
    }

    /**
     * Recursively list all selected rules within the SelectableItem.
     */
    private List<RuleType> getRules(SelectableItemType item) {
	List<RuleType> rules = new Vector<RuleType>();
	if (item instanceof RuleType) {
	    rules.add((RuleType)item);
	} else if (item instanceof GroupType) {
	    for (SelectableItemType child : ((GroupType)item).getGroupOrRule()) {
		rules.addAll(getRules(child));
	    }
	}
	return rules;
    }
}
