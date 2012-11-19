// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashMap;
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
import org.w3c.dom.Element;

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
import xccdf.schemas.core.Model;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.ProfileType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.ScoreType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.TestResultType;

import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.sce.IProvider;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.arf.Report;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.Profile;
import org.joval.scap.xccdf.TestResult;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.handler.OCILHandler;
import org.joval.scap.xccdf.handler.OVALHandler;
import org.joval.scap.xccdf.handler.SCEHandler;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xml.DOMTools;

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

    private Benchmark xccdf;
    private IPlugin plugin;
    private SystemInfoType sysinfo;
    private Collection<String> platforms;
    private Profile profile;
    private Hashtable<String, Checklist> checklists;
    private File ocilDir;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;
    private Logger logger;
    private boolean verbose;
    private Report arfReport;
    private ObjectFactory factory;

    /**
     * Create an XCCDF Processing Engine using the specified XCCDF document bundle and jOVAL plugin.
     */
    public Engine(Benchmark xccdf, Profile profile, Hashtable<String, Checklist> checklists, File ocilDir,
		  IPlugin plugin, boolean verbose, Report arfReport) {

	this.xccdf = xccdf;
	this.profile = profile;
	this.checklists = checklists;
	this.ocilDir = ocilDir;
	this.plugin = plugin;
	this.verbose = verbose;
	this.arfReport = arfReport;
	logger = XPERT.logger;
	factory = new ObjectFactory();
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

	    HashSet<String> selectedIds = new HashSet<String>();
	    for (RuleType rule : rules) {
		selectedIds.add(rule.getId());
	    }

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
	    } else if (plugin.connect()) {
		List<Element> reports = new ArrayList<Element>();
		TestResultType testResult = initializeResult();

		//
		// Perform the applicability tests, and if applicable, the automated checks
		//
		if (isApplicable(reports)) {
		    logger.info("The target system is applicable to the specified XCCDF");
		    processXccdf(testResult, reports);
		} else {
		    logger.info("The target system is not applicable to the specified XCCDF");
		}
		plugin.disconnect();

		//
		// Print results to the console, and add them to the ARF report.
		//
		HashMap<String, RuleResultType> resultIndex = new HashMap<String, RuleResultType>();
		for (RuleResultType rrt : testResult.getRuleResult()) {
		    resultIndex.put(rrt.getIdref(), rrt);
		}
		for (RuleType rule : listAllRules()) {
		    String ruleId = rule.getId();
		    if (resultIndex.containsKey(ruleId)) {
			logger.info(ruleId + ": " + resultIndex.get(ruleId).getResult());
		    } else {
			//
			// Record unselected/unchecked rules
			//
			RuleResultType rrt = factory.createRuleResultType();
			rrt.setIdref(rule.getId());
			if (selectedIds.contains(ruleId)) {
			    rrt.setResult(ResultEnumType.NOTCHECKED);
			} else {
			    rrt.setResult(ResultEnumType.NOTSELECTED);
			}
			if (rule.isSetCheck()) {
			    for (CheckType check : rule.getCheck()) {
				rrt.getCheck().add(check);
			    }
			}
			testResult.getRuleResult().add(rrt);
		    }
		}
		try {
		    reports.add(DOMTools.toElement(new TestResult(testResult)));
		} catch (Exception e) {
		    logger.severe(LogFormatter.toString(e));
		}
		makeArfReport(reports);
		xccdf.getBenchmark().getTestResult().add(testResult);
	    } else {
		logger.info("Failed to connect to the target system");
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

    /**
     * This is the main routine, from which the selected checks are executed and compiled into the result.
     */
    private void processXccdf(TestResultType testResult, List<Element> reports) {
	testResult.setStartTime(getTimestamp());
	phase = "evaluation";

	//
	// Integrate OCIL results
	//
	if (checklists != null) {
	    OCILHandler ocilHandler = new OCILHandler(xccdf, profile, checklists);
	    ocilHandler.integrateResults(testResult);
	    if (verbose) {
		for (Checklist checklist : checklists.values()) {
		    try {
			reports.add(DOMTools.toElement(checklist));
		    } catch (Exception e) {
			logger.severe(LogFormatter.toString(e));
		    }
		}
	    }
	}

	//
	// Run the OVAL engines
	//
	OVALHandler ovalHandler = null;
	try {
	    ovalHandler = new OVALHandler(xccdf, profile, plugin);
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
		if (verbose) {
		    try {
			reports.add(DOMTools.toElement(engine.getResults()));
		    } catch (Exception e) {
			logger.severe(LogFormatter.toString(e));
		    }
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
	if (plugin instanceof IProvider) {
	    SCEHandler handler = new SCEHandler(xccdf, profile, (IProvider)plugin, plugin.getLogger());
	    if (handler.ruleCount() > 0) {
		logger.info("Evaluating SCE rules");
		handler.integrateResults(testResult);
	    }
	}
	testResult.setEndTime(getTimestamp());

	//
	// Compute scores
	//
	logger.info("Computing scores...");
	HashMap<String, RuleResultType> resultMap = new HashMap<String, RuleResultType>();
	for (RuleResultType rrt : testResult.getRuleResult()) {
	    resultMap.put(rrt.getIdref(), rrt);
	}
	// Note - only selected rules will be in the resultMap at this point
	for (Model model : xccdf.getBenchmark().getModel()) {
	    logger.info("Scoring method: " + model.getSystem());
	    ScoreKeeper sk = computeScore(resultMap, xccdf.getBenchmark().getGroupOrRule(), ScoringModel.fromModel(model));
	    ScoreType scoreType = factory.createScoreType();
	    scoreType.setSystem(model.getSystem());
	    scoreType.setValue(new BigDecimal(Float.toString(sk.getScore())));
	    if (sk instanceof FlatScoreKeeper) {
		scoreType.setMaximum(new BigDecimal(Float.toString(((FlatScoreKeeper)sk).getMaxScore())));
	    }
	    testResult.getScore().add(scoreType);
	}
    }

    private void makeArfReport(List<Element> reports) {
	try {
	    String requestId = arfReport.addRequest(DOMTools.toElement(xccdf));
	    String assetId = arfReport.addAsset(sysinfo);
	    for (Element report : reports) {
		arfReport.addReport(requestId, assetId, report);
	    }
	} catch (Exception e) {
	    logger.severe(LogFormatter.toString(e));
	}
    }

    /**
     * Create a Benchmark.TestResult node, initialized with information gathered from the profile and plugin.
     */
    private TestResultType initializeResult() {
	TestResultType testResult = factory.createTestResultType();
	String id = xccdf.getBenchmark().getBenchmarkId();
	String name = "unknown";
	String namespace = "unknown";
	if (id.startsWith("xccdf_")) {
	    String temp = id.substring(6);
	    int ptr = temp.lastIndexOf("_benchmark_");
	    if (ptr > 0) {
		namespace = temp.substring(0,ptr);
		name = temp.substring(ptr+11);
	    }
	}
	testResult.setTestResultId("xccdf_" + namespace + "_testresult_" + name);
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
	String user = plugin.getUsername();
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
	    sysinfo = plugin.getSystemInfo();
	    if (!testResult.getTarget().contains(sysinfo.getPrimaryHostName())) {
		testResult.getTarget().add(sysinfo.getPrimaryHostName());
	    }
	    for (InterfaceType intf : sysinfo.getInterfaces().getInterface()) {
		if (!testResult.getTargetAddress().contains(intf.getIpAddress())) {
		    testResult.getTargetAddress().add(intf.getIpAddress());
		}
	    }
	} catch (OvalException e) {
	    logger.severe(LogFormatter.toString(e));
	}
	return testResult;
    }

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied.
     */
    private boolean isApplicable(List<Element> reports) {
	phase = "discovery";
	logger.info("Determining system applicability...");

	Collection<String> hrefs = profile.getPlatformDefinitionHrefs();
	if (hrefs.size() == 0) {
	    logger.info("No platforms specified, skipping applicability checks...");
	    return true;
	}

	for (String href : hrefs) {
	    try {
		if (!isApplicable(profile.getDefinitions(href), profile.getPlatformDefinitionIds(href), reports)) {
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

    private boolean isApplicable(IDefinitions cpeOval, List<String> definitions, List<Element> reports) {
	IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, plugin);
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
	    if (verbose) {
		reports.add(DOMTools.toElement(engine.getResults()));
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

    enum ScoringModel {
	DEFAULT("urn:xccdf:scoring:default"),
	FLAT("urn:xccdf:scoring:flat"),
	FLAT_UNWEIGHTED("urn:xccdf:scoring:flat-unweighted"),
	ABSOLUTE("urn:xccdf:scoring:absolute");

	private String uri;

	private ScoringModel(String uri) {
	    this.uri = uri;
	}

	static ScoringModel fromModel(Model model) throws IllegalArgumentException {
	    return fromUri(model.getSystem());
	}

	static ScoringModel fromUri(String uri) throws IllegalArgumentException {
	    for (ScoringModel model : values()) {
		if (model.uri.equals(uri)) {
		    return model;
		}
	    }
	    throw new IllegalArgumentException(uri);
	}
    }

    /**
     * Recursively compute the score of a list of items.
     *
     * @param results a HashMap containing the results of all (i.e., exclusively) the /selected/ rules
     */
    ScoreKeeper computeScore(HashMap<String, RuleResultType> results, List<SelectableItemType> items, ScoringModel model)
		throws IllegalArgumentException {

	switch(model) {
	  case DEFAULT:
	    return DefaultScoreKeeper.compute(results, items);
	  case FLAT:
	    return FlatScoreKeeper.compute(true, results, items);
	  case FLAT_UNWEIGHTED:
	    return FlatScoreKeeper.compute(false, results, items);
	  case ABSOLUTE:
	    return AbsoluteScoreKeeper.compute(results, items);
	  default:
	    throw new IllegalArgumentException(model.toString());
	}
    }

    abstract static class ScoreKeeper {
	HashMap<String, RuleResultType> results;
	float score, count;

	ScoreKeeper(HashMap<String, RuleResultType> results) {
	    this.results = results;
	    score = 0;
	    count = 0;
	}

	float getScore() {
	    return score;
	}
    }

    static class DefaultScoreKeeper extends ScoreKeeper {
	static DefaultScoreKeeper compute(HashMap<String, RuleResultType> results, List<SelectableItemType> items)
		throws IllegalArgumentException {
	    return new DefaultScoreKeeper(new DefaultScoreKeeper(results), items);
	}

	private int accumulator;
	private float weightedScore;

	DefaultScoreKeeper(HashMap<String, RuleResultType> results) {
	    super(results);
	    accumulator = 0;
	    weightedScore = 0;
	}

	DefaultScoreKeeper(DefaultScoreKeeper parent, RuleType rule) {
	    this(parent.results);
	    if (results.containsKey(rule.getId())) {
		switch(results.get(rule.getId()).getResult()) {
		  case NOTAPPLICABLE:
		  case NOTCHECKED:
		  case INFORMATIONAL:
		  case NOTSELECTED:
		    break;
		  case PASS:
		    score++;
		    // fall-thru
		  default:
		    count++;
		    break;
		}
		if (count == 0) {
		    score = 0;
		} else {
		    count = 1;
		    score = 100 * score / count;
		}
		weightedScore = rule.getWeight().intValue() * score;
	    }
	}

	DefaultScoreKeeper(DefaultScoreKeeper parent, GroupType group) {
	    this(parent, group.getGroupOrRule());
	    weightedScore = group.getWeight().intValue() * score;
	}

	DefaultScoreKeeper(DefaultScoreKeeper parent, List<SelectableItemType> items) throws IllegalArgumentException {
	    super(parent.results);
	    for (SelectableItemType item : items) {
		DefaultScoreKeeper child = null;
		if (item instanceof RuleType) {
		    child = new DefaultScoreKeeper(this, (RuleType)item);
		} else if (item instanceof GroupType) {
		    child = new DefaultScoreKeeper(this, (GroupType)item);
		} else {
		    throw new IllegalArgumentException(item.getClass().getName());
		}
		if (child.count != 0) {
		    score += child.weightedScore;
		    count++;
		    accumulator += item.getWeight().intValue();
		}
	    }
	    if (accumulator > 0) {
		score = (score / accumulator);
	    }
	}
    }

    static class FlatScoreKeeper extends ScoreKeeper {
	static FlatScoreKeeper compute(boolean weighted, HashMap<String, RuleResultType> results,
		List<SelectableItemType> items) throws IllegalArgumentException {
	    return new FlatScoreKeeper(new FlatScoreKeeper(weighted, results), items);
	}

	private boolean weighted;
	private float max_score;

	FlatScoreKeeper(boolean weighted, HashMap<String, RuleResultType> results) {
	    super(results);
	    this.weighted = weighted;
	    max_score = 0;
	}

	FlatScoreKeeper(FlatScoreKeeper parent, RuleType rule) {
	    this(parent.weighted, parent.results);
	    if (results.containsKey(rule.getId())) {
		switch(results.get(rule.getId()).getResult()) {
		  case NOTAPPLICABLE:
		  case NOTCHECKED:
		  case INFORMATIONAL:
		  case NOTSELECTED:
		    break;
		  case PASS:
		    score++;
		    // fall-thru
		  default:
		    count++;
		    break;
		}
		if (count != 0) {
		    int weight = rule.getWeight().intValue();
		    if (weighted) {
			max_score += weight;
			score = (weight * score / count);
		    } else {
			if (weight == 0) {
			    score = 0;
			} else {
			    score = (score / count);
			}
		    }
		}
	    }
	}

	FlatScoreKeeper(FlatScoreKeeper parent, List<SelectableItemType> items) throws IllegalArgumentException {
	    this(parent.weighted, parent.results);
	    for (SelectableItemType item : items) {
		FlatScoreKeeper child = null;
		if (item instanceof RuleType) {
		    child = new FlatScoreKeeper(this, (RuleType)item);
		} else if (item instanceof GroupType) {
		    child = new FlatScoreKeeper(this, ((GroupType)item).getGroupOrRule());
		} else {
		    throw new IllegalArgumentException(item.getClass().getName());
		}
		score += child.score;
		max_score += child.max_score;
	    }
	}

	float getMaxScore() {
	    return max_score;
	}
    }

    static class AbsoluteScoreKeeper extends FlatScoreKeeper {
	static AbsoluteScoreKeeper compute(HashMap<String, RuleResultType> results, List<SelectableItemType> items)
		throws IllegalArgumentException {
	    return new AbsoluteScoreKeeper(new AbsoluteScoreKeeper(results), items);
	}

	AbsoluteScoreKeeper(HashMap<String, RuleResultType> results) {
	    super(true, results);
	}

	AbsoluteScoreKeeper(AbsoluteScoreKeeper parent, List<SelectableItemType> items) throws IllegalArgumentException {
	    super(parent, items);
	}

	@Override
	float getScore() {
	    if (super.getScore() == getMaxScore()) {
		return 1;
	    } else {
		return 0;
	    }
	}
    }
}
