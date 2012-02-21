// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
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
import xccdf.schemas.core.Benchmark;
import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.SelStringType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.URIidrefType;
import xccdf.schemas.core.ValueType;

import org.joval.cpe.CpeException;
import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.oval.IVariables;
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
    private static final File ws = new File("artifacts");
    private static Logger logger;

    static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.factories.common.createGeneratorType();
	generator.setProductName("jOVAL XPERT");
	generator.setProductVersion(JOVALSystem.getSystemProperty("Alpha"));
	generator.setSchemaVersion("5.10.1");
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

	    engine.setDefinitions(xccdf.getOval());
	    engine.setExternalVariables(createVariables());
	    engine.setDefinitionFilter(createFilter());

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

	    engine.run();
	    switch(engine.getResult()) {
	      case ERR:
		logger.severe(LogFormatter.toString(engine.getError()));
		return;
	      case OK:
		engine.getResults().writeXML(new File(ws, "results.xml"));
		break;
	    }

	    Hashtable<String, ResultEnumeration> results = new Hashtable<String, ResultEnumeration>();
	    for (DefinitionType def :
		 engine.getResults().getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()) {

		results.put(def.getDefinitionId(), def.getResult());
	    }

	    for (RuleType rule : getRules()) {
		String ruleId = rule.getItemId();

		List<CheckType> checks = rule.getCheck();
		if (checks == null) {
		    System.out.println("Skipping rule " + ruleId + ": no checks");
		    continue;
		}
		for (CheckType check : rule.getCheck()) {
		    for (CheckContentRefType ref : check.getCheckContentRef()) {
			String definitionId = ref.getName();
			if (definitionId == null) {
			    logger.info(ruleId + ": OVAL definition UNDEFINED");
			    continue;
			}
			switch (results.get(ref.getName())) {
			  case ERROR:
			    logger.info(ruleId + ": " + ResultEnumType.ERROR);
			    break;

			  case FALSE:
			    logger.info(ruleId + ": " + ResultEnumType.FAIL);
			    break;

			  case TRUE:
			    logger.info(ruleId + ": " + ResultEnumType.PASS);
			    break;

			  case UNKNOWN:
			  default:
			    logger.info(ruleId + ": " + ResultEnumType.UNKNOWN);
			    break;
			}
		    }
		}
	    }
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
		    System.out.println("Passed def " + def.getDefinitionId());
		    break;
		  default:
		    System.out.println("Bad result " + def.getResult() + " for definition " + def.getDefinitionId());
		    return false;
		}
	    } else {
		System.out.println("Ignoring definition " + def.getDefinitionId());
	    }
	}
	return true;
    }

    private IDefinitionFilter createFilter() {
	DefinitionFilter filter = new DefinitionFilter();

	//
	// Get every check from every rule, and add the names to the filter.
	//
	for (RuleType rule : getRules()) {
	    for (CheckType check : rule.getCheck()) {
		if (check.getSystem().equals("http://oval.mitre.org/XMLSchema/oval-definitions-5")) {
		    for (CheckContentRefType ref : check.getCheckContentRef()) {
			filter.addDefinition(ref.getName());
		    }
		}
	    }
	}
	return filter;
    }

    private IVariables createVariables() {
	Variables variables = new Variables(getGenerator());

	//
	// Iterate through Value nodes and get the values without selectors.
	//
	Hashtable<String, String> settings = new Hashtable<String, String>();
	for (ValueType val : xccdf.getBenchmark().getValue()) {
	    for (SelStringType sel : val.getValue()) {
		if (!sel.isSetSelector()) {
		    settings.put(val.getItemId(), sel.getValue());
		}
	    }
	}

	//
	// Get every export from every rule, and add exported values to the OVAL variables.
	//
	for (RuleType rule : getRules()) {
	    for (CheckType check : rule.getCheck()) {
		if (check.getSystem().equals("http://oval.mitre.org/XMLSchema/oval-definitions-5")) {
		    for (CheckExportType export : check.getCheckExport()) {
			String ovalVariableId = export.getExportName();
			List<String> values = new Vector<String>();
			values.add(settings.get(export.getValueId()));
			variables.setValue(export.getExportName(), values);
		    }
		}
	    }
	}
	return variables;
    }

    /**
     * Recursively get all the rules within the XCCDF document.
     */
    private List<RuleType> getRules() {
	if (rules == null) {
	    rules = new Vector<RuleType>();
	    for (SelectableItemType item : xccdf.getBenchmark().getGroupOrRule()) {
		for (RuleType rule : getRules(item)) {
		    rules.add(rule);
		}
	    }
	}
	return rules;
    }

    /**
     * Recursively list all rules within the SelectableItem.
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
