// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
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
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.oval.OvalFactory;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.sce.SCEScript;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.handler.OVALHandler;
import org.joval.xccdf.handler.SCEHandler;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) engine class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements Runnable, IObserver {
    private boolean debug;
    private XccdfBundle xccdf;
    private IBaseSession session;
    private Collection<String> platforms;
    private Profile profile;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;
    private Logger logger;

    /**
     * Create an XCCDF Processing Engine using the specified XCCDF document bundle and jOVAL session.
     */
    public Engine(XccdfBundle xccdf, Profile profile, IBaseSession session, boolean debug) {
	this.xccdf = xccdf;
	this.profile = profile;
	this.session = session;
	logger = XPERT.logger;
	this.debug = debug;
    }

    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
	if (hasSelection() && isApplicable()) {
	    phase = "evaluation";

	    //
	    // Run the OVAL engines
	    //
	    OVALHandler ovalHandler = null;
	    try {
		ovalHandler = new OVALHandler(xccdf, profile, session);
	    } catch (Exception e) {
		logger.severe(LogFormatter.toString(e));
		return;
	    }
	    for (String href : ovalHandler.getHrefs()) {
		IEngine engine = ovalHandler.getEngine(href);
		engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
		logger.info("Evaluating OVAL rules");
		engine.run();
		switch(engine.getResult()) {
		  case OK:
		    if (debug) {
			File resultFile = new File(XPERT.ws, "oval-res_" + encode(href) + ".xml");
			logger.info("Saving OVAL results: " + resultFile.getPath());
			engine.getResults().writeXML(resultFile);
		    }
		    break;
		  case ERR:
		    logger.severe(LogFormatter.toString(engine.getError()));
		    return;
		}
		engine.getNotificationProducer().removeObserver(this);
	    }

	    //
	    // Run the SCE scripts
	    //
	    SCEHandler sceHandler = null;
	    logger.info("Evaluating SCE rules");
	    if (session instanceof ISession) {
		ISession s = (ISession)session;
		if (s.connect()) {
		    sceHandler = new SCEHandler(xccdf, profile, s);
		    for (SCEScript script : sceHandler.getScripts()) {
			logger.info("Running SCE script: " + getFile(script.getSource()));
			if (!script.exec()) {
			    logger.warning("SCE script execution failed!");
			}
		    } 
		    s.disconnect();
		}
	    }

	    //
	    // Create the Benchmark.TestResult node
	    //
	    ObjectFactory factory = new ObjectFactory();
	    TestResultType testResult = factory.createTestResultType();
	    testResult.setTestSystem(XPERT.getMessage("product.name"));
	    try {
		ovalHandler.integrateResults(testResult);
	    } catch (OvalException e) {
		logger.severe(LogFormatter.toString(e));
	    }
	    if (sceHandler != null) {
		sceHandler.integrateResults(testResult);
	    }

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
	    File reportFile = new File(XPERT.ws, "xccdf-results.xml");
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
	    // no-op
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

	Collection<String> definitions = profile.getPlatformDefinitionIds();
	if (definitions.size() == 0) {
	    logger.info("No platforms specified, skipping applicability checks...");
	    return true;
	}
	IDefinitions cpeOval = xccdf.getCpeOval();
	if (cpeOval == null) {
	    logger.severe("Cannot perform applicability checks: CPE OVAL definitions were not found in the XCCDF bundle!");
	    return false;
	}
	IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, session);
	engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
	engine.setDefinitions(xccdf.getCpeOval());
	try {
	    session.connect();
	    for (String definition : definitions) {
		ResultEnumeration result = engine.evaluateDefinition(definition);
		switch(result) {
		  case TRUE:
		    logger.info("Passed def " + definition);
		    break;

		  default:
		    logger.warning("Bad result " + result + " for definition " + definition);
		    logger.info("The target system is not applicable to the XCCDF bundle");
		    return false;
		}
	    }
	    logger.info("The target system is applicable to the XCCDF bundle");
	    return true;
	} catch (Exception e) {
	    logger.severe(LogFormatter.toString(e));
	    return false;
	} finally {
	    session.disconnect();
	    engine.getNotificationProducer().removeObserver(this);
	}
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

    private String encode(String s) {
	return s.replace(System.getProperty("file.separator"), "~");
    }

    private String getFile(URL url) {
	String s = url.toString();
	int ptr = s.lastIndexOf("/");
	if (ptr == -1) {
	    return s;
	} else {
	    return s.substring(ptr+1);
	}
    }
}
