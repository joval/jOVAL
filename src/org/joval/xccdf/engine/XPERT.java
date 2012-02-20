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
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT implements Runnable, IObserver {
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
	File f = null;
	RemoteContainer container = null;

	if (argv.length > 0) {
	    //
	    // Use the specified directory
	    //
	    f = new File(argv[0]);

	    //
	    // Create and configure the RemoteContainer
	    //
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
		    e.printStackTrace();
		    container = null;
		}
	    }
	}

	if (f == null || container == null) {
	    System.out.println("Usage: java org.joval.xccdf.XPERT [path-to-xccdf] [-config path-to-config]");
	    System.out.println(new RemoteContainer().getProperty("helpText"));
	    System.exit(1);
	}

	try {
	    XPERT engine = new XPERT(new XccdfBundle(f), container.getPlugin());
	    engine.run();
	} catch (CpeException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (OvalException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (XccdfException e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    private XccdfBundle xccdf;
    private IEngine engine;
    private List<RuleType> rules = null;

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
	    System.out.println("Beginning scan");
	    break;
	  case IEngine.MESSAGE_OBJECT:
	    System.out.println("Scanning object " + (String)arg);
	    break;
	  case IEngine.MESSAGE_OBJECT_PHASE_END:
	    System.out.println("Scan complete");
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_START:
	    System.out.println("Evaluating definitions");
	    break;
	  case IEngine.MESSAGE_DEFINITION:
	    System.out.println("Evaluating " + (String)arg);
	    break;
	  case IEngine.MESSAGE_DEFINITION_PHASE_END:
	    System.out.println("Completed evaluating definitions");
	    break;
	}
    }

    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
	if (isApplicable()) {
	    System.out.println("The target system is applicable to the XCCDF bundle, continuing tests...");

	    engine.setDefinitions(xccdf.getOval());
	    engine.setExternalVariables(createVariables());
	    engine.setDefinitionFilter(createFilter());
	    engine.run();

engine.getResults().writeXML(new File("results.xml"));

	    Hashtable<String, ResultEnumeration> results = new Hashtable<String, ResultEnumeration>();
	    for (DefinitionType def :
		 engine.getResults().getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()) {

		results.put(def.getDefinitionId(), def.getResult());
	    }

	    for (RuleType rule : getRules()) {
		String ruleId = rule.getItemId();

		for (CheckType check : rule.getCheck()) {
		    for (CheckContentRefType ref : check.getCheckContentRef()) {
			switch (results.get(ref.getName())) {
			  case ERROR:
			    System.out.println(ruleId + ": " + ResultEnumType.ERROR);
			    break;

			  case FALSE:
			    System.out.println(ruleId + ": " + ResultEnumType.FAIL);
			    break;

			  case TRUE:
			    System.out.println(ruleId + ": " + ResultEnumType.PASS);
			    break;

			  case UNKNOWN:
			  default:
			    System.out.println(ruleId + ": " + ResultEnumType.UNKNOWN);
			    break;
			}
		    }
		}
	    }
	} else {
	    System.out.println("The target system is not applicable to the XCCDF bundle");
	}
    }

    // Private

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied.
     */
    private boolean isApplicable() {
	System.out.println("Determining system applicability...");

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
