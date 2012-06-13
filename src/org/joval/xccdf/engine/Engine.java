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
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import oval.schemas.variables.core.VariableType;

import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.CPE2IdrefType;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.IdrefType;
import xccdf.schemas.core.IdentityType;
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
import org.joval.ocil.Checklist;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.oval.OvalFactory;
import org.joval.oval.sysinfo.SysinfoFactory;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.sce.SCEScript;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xccdf.Benchmark;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.handler.OCILHandler;
import org.joval.xccdf.handler.OVALHandler;
import org.joval.xccdf.handler.SCEHandler;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) engine class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements Runnable, IObserver {
    private static DatatypeFactory datatypeFactory = null;
    static {
	try {
	    datatypeFactory = DatatypeFactory.newInstance();
	} catch (DatatypeConfigurationException e) {
	    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private boolean debug;
    private Benchmark xccdf;
    private IBaseSession session;
    private Collection<String> platforms;
    private Profile profile;
    private Hashtable<String, Checklist> checklists;
    private File ocilDir;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;
    private Logger logger;
    private File ws = null;

    /**
     * Create an XCCDF Processing Engine using the specified XCCDF document bundle and jOVAL session.
     */
    public Engine(Benchmark xccdf, Profile profile, Hashtable<String, Checklist> checklists, File ocilDir,
		  IBaseSession session, File ws) {

	this.xccdf = xccdf;
	this.profile = profile;
	this.checklists = checklists;
	this.ocilDir = ocilDir;
	this.session = session;
	this.ws = ws;
	debug = ws != null;
	logger = XPERT.logger;
    }

    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
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
	} else {
	    logger.info("There are " + rules.size() + " rules to process for the selected profile");

	    boolean ocilComplete = true;
	    if (checklists.size() == 0) {
		OCILHandler oh = new OCILHandler(xccdf, profile);
		if (oh.exportFiles(ocilDir)) {
		    ocilComplete = false;
		}
	    }
	    if (!ocilComplete) {
		logger.info(  "\n  ***************** ATTENTION *****************\n\n" +
				"  This XCCDF content requires OCIL result data.\n" +
				"  Content has been exported to: " + ocilDir + "\n");
	    } else if (session.connect()) {
		//
		// Create the Benchmark.TestResult node
		//
		ObjectFactory factory = new ObjectFactory();
		TestResultType testResult = factory.createTestResultType();
		testResult.setVersion(xccdf.getBenchmark().getVersion().getValue());
		testResult.setTestSystem(XPERT.getMessage("product.name"));
		TestResultType.Benchmark trb = factory.createTestResultTypeBenchmark();
		trb.setId(xccdf.getBenchmark().getBenchmarkId());
		trb.setHref(xccdf.getHref());
		testResult.setBenchmark(trb);
		if (profile.getName() != null) {
		    IdrefType profileRef = factory.createIdrefType();
		    profileRef.setIdref(profile.getName());
		    testResult.setProfile(profileRef);
		}
		String user = session.getUsername();
		if (user != null) {
		    IdentityType identity = factory.createIdentityType();
		    identity.setValue(user);
		    if ("root".equals(user) || "Administrator".equals(user)) {
			identity.setPrivileged(true);
		    }
		    identity.setAuthenticated(true);
		    testResult.setIdentity(identity);
		}
		for (String href : profile.getCpePlatforms()) {
		    CPE2IdrefType cpeRef = factory.createCPE2IdrefType();
		    cpeRef.setIdref(href);
		    testResult.getPlatform().add(cpeRef);
		}
		try {
		    SystemInfoType info = SysinfoFactory.createSystemInfo(session);
		    if (!testResult.getTarget().contains(info.getPrimaryHostName())) {
			testResult.getTarget().add(info.getPrimaryHostName());
		    }
		    for (InterfaceType intf : info.getInterfaces().getInterface()) {
			if (!testResult.getTargetAddress().contains(intf.getIpAddress())) {
			    testResult.getTargetAddress().add(intf.getIpAddress());
			}
		    }
		} catch (OvalException e) {
		    logger.severe(LogFormatter.toString(e));
		}

		//
		// Perform the applicability tests, and if applicable, the automated checks
		//
		testResult.setStartTime(getTimestamp());
		if (isApplicable()) {
		    logger.info("The target system is applicable to the specified XCCDF");
		    processXccdf(testResult);
		} else {
		    logger.info("The target system is not applicable to the specified XCCDF");
		}
		session.disconnect();
		testResult.setEndTime(getTimestamp());

		//
		// Print results to the console, and save them to a file.
		//
		Hashtable<String, RuleResultType> resultIndex = new Hashtable<String, RuleResultType>();
		for (RuleResultType rrt : testResult.getRuleResult()) {
		    resultIndex.put(rrt.getIdref(), rrt);
		}
		for (RuleType rule : listAllRules()) {
		    String ruleId = rule.getId();
		    if (resultIndex.containsKey(ruleId)) {
			logger.info(ruleId + ": " + resultIndex.get(ruleId).getResult());
		    } else {
			//
			// Add the unchecked result, just for fun.
			//
			RuleResultType rrt = factory.createRuleResultType();
			rrt.setIdref(rule.getId());
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
	    } else {
		logger.info("Failed to connect to the target system: " + session.getHostname());
	    }
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

    private void processXccdf(TestResultType testResult) {
	phase = "evaluation";

	//
        // Integrate OCIL results
	//
	if (checklists != null) {
	    OCILHandler ocilHandler = new OCILHandler(xccdf, profile, checklists);
	    ocilHandler.integrateResults(testResult);
	}

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
		    String basename = encode(href);
		    if (!basename.toLowerCase().endsWith(".xml")) {
			basename = basename + ".xml";
		    }
		    File resultFile = new File(ws, "oval-res_" + basename);
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
	try {
	    ovalHandler.integrateResults(testResult);
	} catch (OvalException e) {
	    logger.severe(LogFormatter.toString(e));
	}

	//
	// Run the SCE scripts
	//
	logger.info("Evaluating SCE rules");
	if (session instanceof ISession) {
	    ISession s = (ISession)session;
	    SCEHandler sceHandler = new SCEHandler(xccdf, profile, s);
	    for (SCEScript script : sceHandler.getScripts()) {
		logger.info("Running SCE script: " + script.getId());
		if (!script.exec()) {
		    logger.warning("SCE script execution failed!");
		}
	    } 
	    sceHandler.integrateResults(testResult);
	}
    }

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied.
     */
    private boolean isApplicable() {
	phase = "discovery";
	logger.info("Determining system applicability...");

	Collection<String> hrefs = profile.getPlatformDefinitionHrefs();
	if (hrefs.size() == 0) {
	    logger.info("No platforms specified, skipping applicability checks...");
	    return true;
	}

	for (String href : hrefs) {
	    try {
		if (!isApplicable(profile.getDefinitions(href), profile.getPlatformDefinitionIds(href))) {
		    return false;
		}
	    } catch (OvalException e) {
		logger.severe(LogFormatter.toString(e));
		return false;
	    } catch (NoSuchElementException e) {
		logger.severe("Cannot perform applicability checks: CPE OVAL definitions " + e.getMessage() +
			      " were not found in the XCCDF datastream!");
		return false;
	    }
	}
	return true;
    }

    private boolean isApplicable(IDefinitions cpeOval, List<String> definitions) {
	IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, session);
	engine.getNotificationProducer().addObserver(this, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
	engine.setDefinitions(cpeOval);
	try {
	    for (String definition : definitions) {
		ResultEnumeration result = engine.evaluateDefinition(definition);
		switch(result) {
		  case TRUE:
		    logger.info("Passed def " + definition);
		    break;

		  default:
		    logger.warning("Bad result " + result + " for definition " + definition);
		    return false;
		}
	    }
	    return true;
	} catch (Exception e) {
	    logger.severe(LogFormatter.toString(e));
	    return false;
	} finally {
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

    private XMLGregorianCalendar getTimestamp() {
	XMLGregorianCalendar tm = null;
	if (datatypeFactory != null) {
	    tm = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar());
	}
	return tm;
    }
}
