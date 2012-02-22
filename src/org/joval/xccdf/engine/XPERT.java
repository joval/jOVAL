// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.GregorianCalendar;
import java.util.Hashtable;
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
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.SelStringType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.TestResultType;
import xccdf.schemas.core.URIidrefType;
import xccdf.schemas.core.ValueType;

import org.joval.cpe.CpeException;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.DefinitionFilter;
import org.joval.oval.OvalException;
import org.joval.oval.Variables;
import org.joval.oval.di.RemoteContainer;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT implements Runnable, IObserver {
    public static final String OVAL_SYSTEM = "http://oval.mitre.org/XMLSchema/oval-definitions-5";

    private static final File ws = new File("artifacts");
    private static Logger logger;

    private static PropertyResourceBundle resources;
    static {
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
     * Retrieve a message using its key.
     */
    static String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    static final GeneratorType getGenerator() {
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

    /**
     * Run from the command-line.
     */
    public static void main(String[] argv) {
	int exitCode = 1;
	try {
	    if (!ws.exists()) {
		ws.mkdir();
	    }
	    logger = LogFormatter.createDuplex(new File(ws, "xpert.log"), Level.INFO);

	    File f = null;
	    RemoteContainer container = null;
	    if (argv.length > 0) {
		f = new File(argv[0]);
		File configFile = null;
		if (argv.length > 2 && argv[1].equals("-config")) {
		    configFile = new File(argv[2]);
		} else {
		    configFile = new File("config.properties");
		}
		if (configFile.isFile()) {
		    Properties config = new Properties();
		    try {
			config.load(new FileInputStream(configFile));
			container = new RemoteContainer();
			container.configure(config);
		    } catch (Exception e) {
			logger.severe(LogFormatter.toString(e));
			container = null;
		    }
		}
	    }

	    if (f == null || container == null) {
		System.out.println("Usage: java org.joval.xccdf.XPERT [path-to-xccdf] [-config path-to-config]");
		System.out.println(new RemoteContainer().getProperty("helpText"));
	    } else {
		logger.info("Loading " + f.getPath());
		XPERT engine = new XPERT(new XccdfBundle(f), container.getPlugin());
		engine.run();
		exitCode = 0;
	    }
	} catch (Exception e) {
	    logger.warning(LogFormatter.toString(e));
	}

	System.exit(exitCode);
    }

    private XccdfBundle xccdf;
    private IEngine engine;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;

    /**
     * Create an XCCDF Processing Engine and Report Tool using the specified XCCDF document bundle and jOVAL plugin.
     */
    public XPERT(XccdfBundle xccdf, IPlugin plugin) {
	this.xccdf = xccdf;
	engine = JOVALSystem.createEngine(plugin);
	engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
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
	    ((ISystemCharacteristics)arg).writeXML(new File(ws, "sc-" + phase + ".xml"));
	    break;
	}
    }

    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
	if (isApplicable()) {
	    phase = "evaluation";
	    logger.info("The target system is applicable to the XCCDF bundle, continuing tests...");

	    //
	    // Configure the OVAL engine...
	    //
	    engine.setDefinitions(xccdf.getOval());
	    Variables exports = createVariables();
	    engine.setExternalVariables(exports);
	    DefinitionFilter filter = createFilter();
	    engine.setDefinitionFilter(filter);

	    //
	    // Write artifact files
	    //
	    File exportsFile = new File(ws, "oval-variables.xml");
	    logger.info("Saving variables: " + exportsFile.getPath());
	    exports.writeXML(exportsFile);
	    File filterFile = new File(ws, "oval-filter.xml");
	    logger.info("Saving evaluation-definitions: " + filterFile.getPath());
	    filter.writeXML(filterFile);

	    //
	    // Read previously collected system characteristics data, if available.
	    //
	    try {
		File sc = new File(ws, "sc-input.xml");
		if(sc.isFile()) {
		    logger.info("Loading " + sc);
		    engine.setSystemCharacteristicsFile(sc);
		}
	    } catch (OvalException e) {
		logger.warning(e.getMessage());
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
		results.writeXML(new File(ws, "oval-results.xml"));
		break;
	    }

	    //
	    // Create the Benchmark.TestResult node
	    //
	    ObjectFactory factory = new ObjectFactory();
	    TestResultType testResult = factory.createTestResultType();
	    testResult.setTestSystem(getMessage("product.name"));
	    testResult.getTarget().add(results.getSystemInfo().getPrimaryHostName());
	    for (InterfaceType intf : results.getSystemInfo().getInterfaces().getInterface()) {
		testResult.getTargetAddress().add(intf.getIpAddress());
	    }
	    for (VariableType var : exports.getOvalVariables().getVariables().getVariable()) {
		ProfileSetValueType val = factory.createProfileSetValueType();
		val.setIdref(var.getComment());
		val.setValue(var.getValue().get(0).toString());
		testResult.getSetValue().add(val);
	    }

	    //
	    // Iterate through the rules and record the results, save and finish
	    //
	    List<RuleType> rules = getRules(false);
	    for (RuleType rule : rules) {
		String ruleId = rule.getItemId();

		RuleResultType ruleResult = factory.createRuleResultType();
		ruleResult.setIdref(ruleId);

		List<CheckType> checks = rule.getCheck();
		if (checks == null) {
		    logger.info("Skipping rule " + ruleId + ": no checks");
		} else {
		    for (CheckType check : rule.getCheck()) {
			ruleResult.getCheck().add(check);

			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    String definitionId = ref.getName();
			    if (definitionId == null) {
				logger.info(ruleId + ": OVAL definition UNDEFINED");
				continue;
			    }
			    ResultEnumType ret = ResultEnumType.NOTCHECKED;
			    try {
				switch (results.getDefinitionResult(ref.getName())) {
				  case ERROR:
				    ret = ResultEnumType.ERROR;
				    break;

				  case FALSE:
				    ret = ResultEnumType.FAIL;
				    break;

				  case TRUE:
				    ret = ResultEnumType.PASS;
				    break;

				  case UNKNOWN:
				  default:
				    ret = ResultEnumType.UNKNOWN;
				    break;
				}
			    } catch (NoSuchElementException e) {
			    }
			    logger.info(ruleId + ": " + ret);
			    ruleResult.setResult(ret);
			}
		    }
		}
		testResult.getRuleResult().add(ruleResult);
	    }
	    xccdf.getBenchmark().getTestResult().add(testResult);
	    File reportFile = new File(ws, "xccdf-results.xml");
	    logger.info("Saving report: " + reportFile.getPath());
	    xccdf.writeBenchmarkXML(reportFile);
	    logger.info("XCCDF processing complete.");
	} else {
	    logger.info("The target system is not applicable to the XCCDF bundle");
	}
    }

    // Private

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied.
     */
    private boolean isApplicable() {
	phase = "discovery";
	logger.info("Determining system applicability...");

	//
	// Create a DefinitionFilter containing all the OVAL definitions corresponding to the CPE platforms.
	//
	DefinitionFilter filter = new DefinitionFilter();
	for (URIidrefType platform : xccdf.getBenchmark().getPlatform()) {
	    filter.addDefinition(xccdf.getDictionary().getOvalDefinition(platform.getIdref()));
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
		    return false;
		}
	    } else {
		logger.info("Ignoring definition " + def.getDefinitionId());
	    }
	}
	return true;
    }

    /**
     * Create an OVAL DefinitionFilter containing every selected rule.
     */
    private DefinitionFilter createFilter() {
	DefinitionFilter filter = new DefinitionFilter();

	//
	// Get every check from every selected rule, and add the names to the filter.
	//
	for (RuleType rule : getRules(true)) {
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
	return filter;
    }

    /**
     * Gather all the variable exports into a single OVAL variables structure.
     */
    private Variables createVariables() {
	Variables variables = new Variables(getGenerator());

	//
	// Iterate through all selected Value nodes and get the values without selectors.
	//
	Hashtable<String, String> settings = new Hashtable<String, String>();
	for (ValueType val : xccdf.getBenchmark().getValue()) {
	    for (SelStringType sel : val.getValue()) {
		if (!sel.isSetSelector()) {
		    settings.put(val.getItemId(), sel.getValue());
		}
	    }
	}
	for (GroupType group : getSelectedGroups()) {
	    if (group.isSetValue()) {
		logger.info("Getting Values for group " + group.getItemId());
		for (ValueType val : group.getValue()) {
		    if (val.isSetValue()) {
			logger.info("Getting values of Value " + val.getItemId());
			for (SelStringType sel : val.getValue()) {
			    if (!sel.isSetSelector()) {
				settings.put(val.getItemId(), sel.getValue());
			    }
			}
		    } else {
			logger.info("Value " + val.getItemId() + " has no values");
		    }
		}
	    } else {
		logger.info("Group " + group.getItemId() + " has no values");
	    }
	}

	//
	// Get every export from every selected rule, and add exported values to the OVAL variables.
	// REMIND (DAS): are multi-valued exports possible?
	//
	for (RuleType rule : getRules(true)) {
	    for (CheckType check : rule.getCheck()) {
		if (check.getSystem().equals(OVAL_SYSTEM)) {
		    for (CheckExportType export : check.getCheckExport()) {
			String ovalVariableId = export.getExportName();
			List<String> values = new Vector<String>();
			values.add(settings.get(export.getValueId()));
			variables.setValue(ovalVariableId, values);
			variables.setComment(ovalVariableId, export.getValueId());
		    }
		}
	    }
	}
	return variables;
    }

    /**
     * Recursively get all selected groups within the XCCDF document.
     */
    private List<GroupType> getSelectedGroups() {
	if (groups == null) {
	    groups = new Vector<GroupType>();
	    for (SelectableItemType item : xccdf.getBenchmark().getGroupOrRule()) {
		for (GroupType group : getSelectedGroups(item)) {
		    groups.add(group);
		}
	    }
	}
	return groups;
    }

    /**
     * Recursively list all selected groups within the SelectableItem.
     */
    private List<GroupType> getSelectedGroups(SelectableItemType item) {
	List<GroupType> groups = new Vector<GroupType>();
	if (item.isSelected() && item instanceof GroupType) {
	    groups.add((GroupType)item);
	    for (SelectableItemType child : ((GroupType)item).getGroupOrRule()) {
		groups.addAll(getSelectedGroups(child));
	    }
	}
	return groups;
    }

    /**
     * Recursively get rules within the XCCDF document.
     *
     * @arg selectedOnly if true, only selected rules are returned
     */
    private List<RuleType> getRules(boolean selectedOnly) {
	if (rules == null) {
	    rules = new Vector<RuleType>();
	    for (SelectableItemType item : xccdf.getBenchmark().getGroupOrRule()) {
		for (RuleType rule : getRules(item, selectedOnly)) {
		    rules.add(rule);
		}
	    }
	}
	return rules;
    }

    /**
     * Recursively list all selected rules within the SelectableItem.
     */
    private List<RuleType> getRules(SelectableItemType item, boolean selectedOnly) {
	List<RuleType> rules = new Vector<RuleType>();
	if (!selectedOnly || item.isSelected()) {
	    if (item instanceof RuleType) {
		rules.add((RuleType)item);
	    } else if (item instanceof GroupType) {
		for (SelectableItemType child : ((GroupType)item).getGroupOrRule()) {
		    rules.addAll(getRules(child, selectedOnly));
		}
	    }
	}
	return rules;
    }
}
