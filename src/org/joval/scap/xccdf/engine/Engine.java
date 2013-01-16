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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import org.w3c.dom.Element;

import org.slf4j.cal10n.LocLogger;
import jsaf.intf.system.ISession;

import scap.cpe.dictionary.ListType;
import scap.datastream.Component;
import scap.oval.common.GeneratorType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.results.DefinitionType;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.oval.variables.VariableType;
import scap.xccdf.BenchmarkType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CPE2IdrefType;
import scap.xccdf.GroupType;
import scap.xccdf.IdrefType;
import scap.xccdf.IdentityType;
import scap.xccdf.Model;
import scap.xccdf.ObjectFactory;
import scap.xccdf.ProfileSetValueType;
import scap.xccdf.ProfileType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.ScoreType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.xml.ITransformable;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.arf.ArfException;
import org.joval.scap.arf.Report;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.oval.sysinfo.SysinfoFactory;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.TestResult;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.handler.OCILHandler;
import org.joval.scap.xccdf.handler.OVALHandler;
import org.joval.scap.xccdf.handler.SCEHandler;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;
import org.joval.xml.DOMTools;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) engine class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements org.joval.intf.scap.xccdf.IEngine {
    private static final String PRODUCT_NAME;
    private static DatatypeFactory datatypeFactory = null;
    static {
	PRODUCT_NAME = JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT) + " " +
		       JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION);
	try {
	    datatypeFactory = DatatypeFactory.newInstance();
	} catch (DatatypeConfigurationException e) {
	    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private enum State {
	CONFIGURE,
	RUNNING,
	COMPLETE_OK,
	COMPLETE_ERR;
    }

    private IDatastream stream;
    private IView view;
    private IBenchmark benchmark;
    private IPlugin plugin;
    private File ocilDir;
    private SystemInfoType sysinfo;
    private Map<String, IChecklist> checklists;
    private List<RuleType> rules = null;
    private List<GroupType> groups = null;
    private String phase = null;
    private LocLogger logger;
    private List<ITransformable> reports;
    private ObjectFactory factory;
    private Exception error;
    private State state = State.CONFIGURE;
    private boolean abort = false;
    private Producer producer;

    /**
     * Create an XCCDF Processing Engine using the specified XCCDF document bundle and jOVAL plugin.
     */
    protected Engine(IPlugin plugin) {
	this.plugin = plugin;
	logger = plugin.getLogger();
	factory = new ObjectFactory();
	producer = new Producer();
	reset();
    }

    // Implement IEngine

    public void destroy() {
	if (state == State.RUNNING) {
	    abort = true;
	}
    }

    public void setView(IView view) throws IllegalStateException, XccdfException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-through

	  default:
	    this.view = view;
	    stream = view.getStream();
	    benchmark = stream.getBenchmark(view.getBenchmark());
	    break;
	}
    }

    public void addChecklist(String href, IChecklist checklist) throws IllegalStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-through

	  default:
	    checklists.put(href, checklist);
	    break;
	}
    }

    public IProducer getNotificationProducer() {
	return producer;
    }

    public Result getResult() throws IllegalThreadStateException {
	switch(state) {
	  case COMPLETE_OK:
	    return Result.OK;

	  case COMPLETE_ERR:
	    return Result.ERR;

	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public IReport getReport(SystemEnumeration... systems) throws IllegalThreadStateException, ArfException {
	switch(state) {
	  case COMPLETE_OK:
	    try {
		Report report = new Report();
		String requestId = report.addRequest(DOMTools.toElement(benchmark));
		String assetId = report.addAsset(sysinfo);
		for (ITransformable subreport : reports) {
		    Element elt = DOMTools.toElement(subreport);
		    String ns = elt.getNamespaceURI();
		    if (SystemEnumeration.XCCDF.namespace().equals(ns)) {
			//
			// Always include the XCCDF report
			//
			report.addReport(requestId, assetId, elt);
		    } else {
			for (SystemEnumeration system : systems) {
			    if (system == SystemEnumeration.ANY || system.namespace().equals(ns)) {
				report.addReport(requestId, assetId, elt);
				break;
			    }
			}
		    }
		}
		return report;
	    } catch (Exception e) {
		throw new ArfException(e);
	    }

	  case COMPLETE_ERR:
	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public Exception getError() throws IllegalThreadStateException {
	switch(state) {
	  case COMPLETE_ERR:
	    return error;

	  case COMPLETE_OK:
	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }


    // Implement Runnable

    /**
     * Process the XCCDF document bundle.
     */
    public void run() {
	state = State.RUNNING;
	boolean doDisconnect = false;
	try {
	    Collection<RuleType> rules = view.getSelectedRules();
	    if (rules.size() == 0) {
		logger.warn(JOVALMsg.WARNING_XCCDF_RULES);
	    } else {
		logger.info(JOVALMsg.STATUS_XCCDF_RULES, rules.size(), view.getProfile());
		HashSet<String> selectedIds = new HashSet<String>();
		for (RuleType rule : rules) {
		    selectedIds.add(rule.getId());
		}
		boolean ocilExports = false;
		if (checklists.size() == 0) {
		    OCILHandler oh = new OCILHandler(view);
		    ocilExports = oh.exportFiles(producer);
		}
		if (ocilExports) {
		    throw new OcilException(JOVALMsg.getMessage(JOVALMsg.ERROR_OCIL_REQUIRED));
		}
		if (!plugin.isConnected()) {
		    if (plugin.connect()) {
			doDisconnect = true;
		    } else {
			throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
		    }
		}
		TestResultType testResult = initializeResult();

		//
		// Perform the applicability tests, and if applicable, the automated checks
		//
		producer.sendNotify(MESSAGE_PLATFORM_PHASE_START, null);
		if (isApplicable()) {
		    producer.sendNotify(MESSAGE_PLATFORM_PHASE_END, Boolean.TRUE);
		    processXccdf(testResult);
		} else {
		    producer.sendNotify(MESSAGE_PLATFORM_PHASE_END, Boolean.FALSE);
		    logger.info(JOVALMsg.WARNING_CPE_TARGET, plugin.getSession().getHostname());
		}
		plugin.disconnect();

		//
		// Print results to the console, and add them to the ARF report
		//
		HashMap<String, RuleResultType> resultIndex = new HashMap<String, RuleResultType>();
		for (RuleResultType rrt : testResult.getRuleResult()) {
		    resultIndex.put(rrt.getIdref(), rrt);
		}
		for (RuleType rule : listAllRules()) {
		    String ruleId = rule.getId();
		    if (resultIndex.containsKey(ruleId)) {
			logger.info(JOVALMsg.STATUS_XCCDF_RULE, ruleId, resultIndex.get(ruleId).getResult());
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
		reports.add(new TestResult(testResult));
		benchmark.getBenchmark().getTestResult().add(testResult);
	    }
	    state = State.COMPLETE_OK;
	} catch (Exception e) {
	    state = State.COMPLETE_ERR;
	    error = e;
	} finally {
	    if (doDisconnect) {
		plugin.disconnect();
	    }
	}
    }

    // Private

    private void reset() {
	ocilDir = new File("ocil-export");
	checklists = new HashMap<String, IChecklist>();
	reports = new ArrayList<ITransformable>();
	state = State.CONFIGURE;
	error = null;
    }

    /**
     * This is the main routine, from which the selected checks are executed and compiled into the result.
     */
    private void processXccdf(TestResultType testResult) throws Exception {
	producer.sendNotify(MESSAGE_RULES_PHASE_START, null);
	testResult.setStartTime(getTimestamp());
	phase = "evaluation";

	//
	// Integrate OCIL results
	//
	if (checklists != null) {
	    OCILHandler ocilHandler = new OCILHandler(view, checklists);
	    ocilHandler.integrateResults(testResult);
	    for (IChecklist checklist : checklists.values()) {
		reports.add(checklist);
	    }
	}

	//
	// Run the OVAL engines
	//
	OVALHandler ovalHandler = new OVALHandler(view, plugin);
	for (String href : ovalHandler.getHrefs()) {
	    IEngine engine = ovalHandler.getEngine(href);
	    producer.sendNotify(MESSAGE_OVAL_ENGINE, engine);
	    engine.run();
	    switch(engine.getResult()) {
	      case OK:
		reports.add(engine.getResults());
		break;
	      case ERR:
		throw engine.getError();
	    }
	}
	ovalHandler.integrateResults(testResult);

	//
	// Run the SCE scripts
	//
	if (plugin.getSession() instanceof ISession) {
	    SCEHandler handler = new SCEHandler(view, (ISession)plugin.getSession(), plugin.getLogger());
	    if (handler.ruleCount() > 0) {
		handler.integrateResults(testResult, producer);
	    }
	}
	testResult.setEndTime(getTimestamp());
	producer.sendNotify(MESSAGE_RULES_PHASE_END, null);

	//
	// Compute scores
	//
	HashMap<String, RuleResultType> resultMap = new HashMap<String, RuleResultType>();
	for (RuleResultType rrt : testResult.getRuleResult()) {
	    resultMap.put(rrt.getIdref(), rrt);
	}
	// Note - only selected rules will be in the resultMap at this point
	BenchmarkType bt = benchmark.getBenchmark();
	for (Model model : bt.getModel()) {
	    ScoreKeeper sk = computeScore(resultMap, bt.getGroupOrRule(), ScoringModel.fromModel(model));
	    ScoreType scoreType = factory.createScoreType();
	    scoreType.setSystem(model.getSystem());
	    String score = Float.toString(sk.getScore());
	    scoreType.setValue(new BigDecimal(score));
	    if (sk instanceof FlatScoreKeeper) {
		scoreType.setMaximum(new BigDecimal(Float.toString(((FlatScoreKeeper)sk).getMaxScore())));
	    }
	    testResult.getScore().add(scoreType);
	    logger.info(JOVALMsg.STATUS_XCCDF_SCORE, model.getSystem(), score);
	}
    }

    /**
     * Create a Benchmark.TestResult node, initialized with information gathered from the view and plugin.
     */
    private TestResultType initializeResult() throws OvalException {
	TestResultType testResult = factory.createTestResultType();
	String id = view.getBenchmark();
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
	testResult.setVersion(benchmark.getBenchmark().getVersion().getValue());
	testResult.setTestSystem(PRODUCT_NAME);

	TestResultType.Benchmark trb = factory.createTestResultTypeBenchmark();
	trb.setId(benchmark.getBenchmark().getBenchmarkId());
	trb.setHref(benchmark.getHref());
	testResult.setBenchmark(trb);
	if (view.getProfile() != null) {
	    IdrefType profileRef = factory.createIdrefType();
	    profileRef.setIdref(view.getProfile());
	    testResult.setProfile(profileRef);
	}
	String user = plugin.getSession().getUsername();
	if (user != null) {
	    IdentityType identity = factory.createIdentityType();
	    identity.setValue(user);
	    if ("root".equals(user) || "Administrator".equals(user)) {
		identity.setPrivileged(true);
	    }
	    identity.setAuthenticated(true);
	    testResult.setIdentity(identity);
	}
	for (String href : view.getCpePlatforms()) {
	    CPE2IdrefType cpeRef = factory.createCPE2IdrefType();
	    cpeRef.setIdref(href);
	    testResult.getPlatform().add(cpeRef);
	}
	sysinfo = SysinfoFactory.createSystemInfo(plugin.getSession());
	if (!testResult.getTarget().contains(sysinfo.getPrimaryHostName())) {
	    testResult.getTarget().add(sysinfo.getPrimaryHostName());
	}
	for (InterfaceType intf : sysinfo.getInterfaces().getInterface()) {
	    if (!testResult.getTargetAddress().contains(intf.getIpAddress())) {
		testResult.getTargetAddress().add(intf.getIpAddress());
	    }
	}
	return testResult;
    }

    /**
     * Test whether the XCCDF bundle's applicability rules are satisfied, i.e., at least one platform is applicable.
     */
    private boolean isApplicable() throws Exception {
	phase = "discovery";
	Collection<String> platforms = view.getCpePlatforms();
	if (platforms.size() == 0) {
	    logger.info("No platforms specified, skipping applicability checks...");
	    return true;
	}
	boolean applicable = false;
	for (String platform : platforms) {
	    producer.sendNotify(MESSAGE_PLATFORM_CPE, platform);
	    try {
		if (isApplicable(view.getCpeOval(platform))) {
		    logger.info(JOVALMsg.STATUS_CPE_TARGET, plugin.getSession().getHostname(), platform);
		    applicable = true;
		}
	    } catch (NoSuchElementException e) {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, e.getMessage()));
	    }
	}
	return applicable;
    }

    /**
     * Check for applicability for a single platform, i.e., all the OVAL definitions in cpeOval evaluate to TRUE.
     */
    private boolean isApplicable(Map<String, IDefinitionFilter> cpeOval) throws Exception {
	for (Map.Entry<String, IDefinitionFilter> entry : cpeOval.entrySet()) {
	    IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, plugin);
	    producer.sendNotify(MESSAGE_OVAL_ENGINE, engine);
	    DefinitionMonitor monitor = new DefinitionMonitor();
	    engine.getNotificationProducer().addObserver(monitor, IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
	    try {
		engine.setDefinitions(stream.getOval(entry.getKey()));
		engine.setDefinitionFilter(entry.getValue());
		engine.run();
		switch(engine.getResult()) {
		  case OK:
		    IResults results = engine.getResults();
		    reports.add(results);
		    for (String definition : monitor.definitions()) {
			switch(results.getDefinitionResult(definition)) {
			  case TRUE:
			    break;

			  default:
			    return false;
			}
		    }
		    break;

		  case ERR:
		    throw engine.getError();
		}
	    } finally {
		engine.getNotificationProducer().removeObserver(monitor);
	    }
	}
	return true;
    }

    /**
     * Recursively get all of the rules within the XCCDF document.
     */
    private List<RuleType> listAllRules() {
	List<RuleType> rules = new Vector<RuleType>();
	for (SelectableItemType item : benchmark.getBenchmark().getGroupOrRule()) {
	    rules.addAll(getRules(item));
	}
	return rules;
    }

    /**
     * Recursively list all of the selected rules within the SelectableItem.
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

    private XMLGregorianCalendar getTimestamp() {
	XMLGregorianCalendar tm = null;
	if (datatypeFactory != null) {
	    tm = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar());
	}
	return tm;
    }

    /**
     * An IObserver for an OVAL IEngine, that tracks all the definitions that are evaluated.
     */
    class DefinitionMonitor implements IObserver {
	private HashSet<String> defs;

	DefinitionMonitor() {
	    defs = new HashSet<String>();
	}

	Collection<String> definitions() {
	    return defs;
	}

	public void notify(IProducer sender, int msg, Object arg) {
	    switch(msg) {
	      case IEngine.MESSAGE_DEFINITION:
		defs.add((String)arg);
		break;
	    }
	}
    }

    /**
     * Enumeration of the XCCDF scoring models.
     */
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

    /**
     * The base class for ScoreKeepers.
     */
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

    /**
     * ScoreKeeper implementation for ScoringModel.DEFAULT.
     */
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

    /**
     * ScoreKeeper implementation for ScoringModel.FLAT and FLAT_UNWEIGHTED.
     */
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

    /**
     * ScoreKeeper implementation for ScoringModel.ABSOLUTE.
     */
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
