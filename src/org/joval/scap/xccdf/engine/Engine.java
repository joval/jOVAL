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
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import org.slf4j.cal10n.LocLogger;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.system.IWindowsSession;

import scap.cpe.dictionary.ListType;
import scap.cpe.language.LogicalTestType;
import scap.cpe.language.CheckFactRefType;
import scap.oval.common.GeneratorType;
import scap.oval.common.OperatorEnumeration;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.results.DefinitionType;
import scap.oval.results.ResultEnumeration;
import scap.oval.systemcharacteristics.core.InterfaceType;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.oval.variables.VariableType;
import scap.xccdf.BenchmarkType;
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckType;
import scap.xccdf.ComplexCheckType;
import scap.xccdf.CPE2IdrefType;
import scap.xccdf.GroupType;
import scap.xccdf.IdrefType;
import scap.xccdf.IdentityType;
import scap.xccdf.Model;
import scap.xccdf.ObjectFactory;
import scap.xccdf.OverrideableCPE2IdrefType;
import scap.xccdf.ProfileSetComplexValueType;
import scap.xccdf.ProfileSetValueType;
import scap.xccdf.ProfileType;
import scap.xccdf.RoleEnumType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.ScoreType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.ocil.IVariables;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.xml.ITransformable;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.arf.ArfException;
import org.joval.scap.arf.Report;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.ocil.Variables;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.oval.engine.OperatorData;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.TestResult;
import org.joval.scap.xccdf.XccdfException;
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
public class Engine implements IXccdfEngine {
    public static final ObjectFactory FACTORY = new ObjectFactory();

    public static Collection<OcilMessageArgument> getOcilExports(IScapContext ctx) throws OcilException {
	Map<String, Collection<String>> qMap = new HashMap<String, Collection<String>>();
	Map<String, Variables> vMap = new HashMap<String, Variables>();
	for (RuleType rule : ctx.getSelectedRules()) {
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    addExports(check, ctx, qMap, vMap);
		}
	    } else if (rule.isSetComplexCheck()) {
		addExports(rule.getComplexCheck(), ctx, qMap, vMap);
	    }
	}

	//
	// Export variables and OCIL XML for each HREF in the context.
	//
	Collection<OcilMessageArgument> results = new ArrayList<OcilMessageArgument>();
	for (Map.Entry<String, Collection<String>> entry : qMap.entrySet()) {
	    String href = entry.getKey();
	    try {
		IChecklist checklist = ctx.getOcil(href);
		IVariables vars = vMap.get(href);
		results.add(new Argument(href, checklist, entry.getValue(), vars));
	    } catch (NoSuchElementException e) {
		throw new OcilException(e);
	    }
	}
	return results;
    }

    private static void addExports(ComplexCheckType check, IScapContext ctx, Map<String, Collection<String>> qMap,
		Map<String, Variables> vMap) throws OcilException {

	for (Object obj : check.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		addExports((CheckType)obj, ctx, qMap, vMap);
	    } else if (obj instanceof ComplexCheckType) {
		addExports((ComplexCheckType)obj, ctx, qMap, vMap);
	    }
	}
    }

    private static void addExports(CheckType check, IScapContext ctx, Map<String, Collection<String>> qMap,
		Map<String, Variables> vMap) throws OcilException {

	if (check.getSystem().equals(OcilHandler.NAMESPACE)) {
	    if (check.isSetCheckContentRef()) {
		Variables vars = null;
		for (CheckContentRefType ref : check.getCheckContentRef()) {
		    String href = ref.getHref();
		    String qId = ref.getName();
		    if (!qMap.containsKey(href)) {
			qMap.put(href, new HashSet<String>());
		    }
		    qMap.get(href).add(qId);
		    if (vMap.containsKey(href)) {
			vars = vMap.get(href);
		    } else {
			vars = new Variables();
			vMap.put(href, vars);
		    }
		    for (CheckExportType export : check.getCheckExport()) {
			String ocilVariableId = export.getExportName();
			String valueId = export.getValueId();
			for (String s : ctx.getValues().get(valueId)) {
			    vars.addValue(ocilVariableId, s);
			}
			vars.setComment(ocilVariableId, valueId);
		    }
		}
	    }
	}
    }

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

    private IScapContext ctx;
    private IPlugin plugin;
    private SystemInfoType sysinfo;
    private Collection<String> applicableCpes;
    private Map<String, IChecklist> checklists;
    private Map<String, IResults> ovalResults;
    private Map<String, IScriptResult> scriptResults;
    private Map<String, Boolean> platforms;
    private IReport report;
    private String requestId;
    private Map<String, ITransformable> subreports;
    private Exception error;
    private State state = State.CONFIGURE;
    private boolean abort = false;
    private Producer<Message> producer;
    private LocLogger logger;

    /**
     * Create an XCCDF Processing Engine using the specified XCCDF document bundle and jOVAL plugin.
     */
    protected Engine(IPlugin plugin) {
	this.plugin = plugin;
	logger = plugin.getLogger();
	producer = new Producer<Message>();
	reset();
    }

    // Implement IXccdfEngine

    public void destroy() {
	if (state == State.RUNNING) {
	    abort = true;
	}
    }

    public void setContext(IScapContext ctx) throws IllegalStateException, XccdfException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-thru
	  default:
	    this.ctx = ctx;	
	    try {
		requestId = report.addRequest(DOMTools.toElement(ctx.getBenchmark()));
	    } catch (Exception e) {
		throw new XccdfException(e);
	    }
	    break;
	}
    }

    public void assertPlatform(String idref, boolean applicable) {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-thru
	  default:
	    platforms.put(idref, applicable ? Boolean.TRUE : Boolean.FALSE);
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
	    // fall-thru
	  default:
	    checklists.put(href, checklist);
	    break;
	}
    }

    public void addOvalResult(String href, IResults results) throws IllegalStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-thru
	  default:
	    ovalResults.put(href, results);
	    break;
	}
    }

    public void addScriptResult(String href, IScriptResult result) throws IllegalStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-thru
	  default:
	    scriptResults.put(href, result);
	    break;
	}
    }

    public IProducer<Message> getNotificationProducer() {
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
		String assetId = report.addAsset(sysinfo, applicableCpes);
		for (Map.Entry<String, ITransformable> entry : subreports.entrySet()) {
		    ITransformable subreport = entry.getValue();
		    String ns = DOMTools.getNamespace(subreport);
		    if (SystemEnumeration.XCCDF.namespace().equals(ns)) {
			//
			// Always include the XCCDF report
			//
			report.addReport(requestId, assetId, entry.getKey(), DOMTools.toElement(subreport));
		    } else {
			for (SystemEnumeration system : systems) {
			    if (system == SystemEnumeration.ANY || system.namespace().equals(ns)) {
				report.addReport(requestId, assetId, entry.getKey(), DOMTools.toElement(subreport));
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
	    List<RuleType> rules = ctx.getSelectedRules();
	    if (rules.size() == 0) {
		logger.warn(JOVALMsg.WARNING_XCCDF_RULES);
	    } else {
		String profileId = null;
		if (ctx.getProfile() == null) {
		    profileId = JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_NOPROFILE);
		} else {
		    profileId = ctx.getProfile().getProfileId();
		}
		logger.info(JOVALMsg.STATUS_XCCDF_RULES, rules.size(), profileId);
		HashSet<String> selectedIds = new HashSet<String>();
		for (RuleType rule : rules) {
		    selectedIds.add(rule.getId());
		}
		if (checklists.size() == 0) {
		    Collection<OcilMessageArgument> exports = getOcilExports(ctx);
		    if (exports.size() > 0) {
			for (OcilMessageArgument arg : exports) {
			    producer.sendNotify(IXccdfEngine.Message.OCIL_MISSING, arg);
			}
			throw new OcilException(JOVALMsg.getMessage(JOVALMsg.ERROR_OCIL_REQUIRED));
		    }
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
		// Perform the profile's platform applicability tests, and if it's applicable, the selected checks
		//
		producer.sendNotify(Message.PLATFORM_PHASE_START, null);
		checkPlatforms(testResult);
		List<CPE2IdrefType> cpes = new ArrayList<CPE2IdrefType>();
		if (ctx.getProfile() == null) {
		    cpes.addAll(ctx.getBenchmark().getBenchmark().getPlatform());
		} else if (ctx.getProfile().getPlatform().size() > 0 && ctx.getProfile().getPlatform().get(0).getOverride()) {
		    cpes.addAll(ctx.getProfile().getPlatform());
		} else {
		    cpes.addAll(ctx.getBenchmark().getBenchmark().getPlatform());
		    cpes.addAll(ctx.getProfile().getPlatform());
		}
		boolean applicable = cpes.size() == 0;
		for (CPE2IdrefType cpe : cpes) {
		    if (platforms.get(cpe.getIdref()).booleanValue()) {
			applicableCpes.add(cpe.getIdref());
			applicable = true;
			break;
		    }
		}
		if (applicable) {
		    producer.sendNotify(Message.PLATFORM_PHASE_END, Boolean.TRUE);
		    processXccdf(testResult);
		} else {
		    testResult.setEndTime(getTimestamp());
		    producer.sendNotify(Message.PLATFORM_PHASE_END, Boolean.FALSE);
		    logger.info(JOVALMsg.WARNING_CPE_TARGET, plugin.getSession().getHostname());
		}
		if (doDisconnect) {
		    plugin.disconnect();
		    doDisconnect = false;
		}

		//
		// Add results for all the rules to the XCCDF results, and add that to the ARF report
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
			// Add a result record for the unselected/unchecked rule
			//
			RuleResultType rrt = FACTORY.createRuleResultType();
			rrt.setIdref(rule.getId());
			if (selectedIds.contains(ruleId)) {
			    rrt.setResult(getRuleResult(rule, ResultEnumType.NOTCHECKED));
			} else {
			    rrt.setResult(getRuleResult(rule, ResultEnumType.NOTSELECTED));
			}
			if (rule.isSetCheck()) {
			    for (CheckType check : rule.getCheck()) {
				rrt.getCheck().add(check);
			    }
			}
			testResult.getRuleResult().add(rrt);
		    }
		}
		subreports.put(testResult.getBenchmark().getHref(), new TestResult(testResult));

		//
		// XPERT requires the result to be added to the benchmark to create the HTML transform
		//
		ctx.getBenchmark().getBenchmark().getTestResult().add(testResult);
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
	checklists = new HashMap<String, IChecklist>();
	ovalResults = new HashMap<String, IResults>();
	scriptResults = new HashMap<String, IScriptResult>();
	platforms = new HashMap<String, Boolean>();
	report = new Report();
	subreports = new HashMap<String, ITransformable>();
	state = State.CONFIGURE;
	error = null;
    }

    /**
     * This is the main routine, from which the selected checks are executed and compiled into the result.
     */
    private void processXccdf(TestResultType testResult) throws Exception {
	producer.sendNotify(Message.RULES_PHASE_START, null);

	Map<String, ISystem> handlers = new HashMap<String, ISystem>();
	if (checklists.size() > 0) {
	    OcilHandler ocil = new OcilHandler(ctx, checklists);
	    ocil.setStartTime(testResult);
	    handlers.put(ocil.getNamespace(), ocil);
	}
	ISystem oval = new OvalHandler(ctx, producer, ovalResults);
	handlers.put(oval.getNamespace(), oval);
	ISystem sce = new SceHandler(ctx, producer, scriptResults);
	handlers.put(sce.getNamespace(), sce);

	//
	// Add all the selected rules to the handlers
	//
	Collection<String> notApplicable = new HashSet<String>();
	for (RuleType rule : ctx.getSelectedRules()) {
	    if (rule.getRole() != RoleEnumType.UNCHECKED && !rule.getAbstract()) {
		//
		// Check that at least one platform applies to the rule
		//
		boolean platformCheck = rule.getPlatform().size() == 0;
		for (OverrideableCPE2IdrefType cpe : rule.getPlatform()) {
		    if (platforms.get(cpe.getIdref()).booleanValue()) {
			platformCheck = true;
			break;
		    }
		}
		if (platformCheck) {
		    if (rule.isSetComplexCheck()) {
			// Per 7.2.3.5.1, process only the complex check if one exists
			addAllChecks(rule.getComplexCheck(), handlers);
		    } else {
			for (CheckType check : rule.getCheck()) {
			    if (handlers.containsKey(check.getSystem())) {
				handlers.get(check.getSystem()).add(check);
				break;
			    }
			}
		    }
		} else {
		    notApplicable.add(rule.getId());
		}
	    }
	}

	//
	// Have all the handlers run their checks
	//
	for (ISystem handler : handlers.values()) {
	    if (abort) {
		throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_ABORT));
	    }
	    subreports.putAll(handler.exec(plugin));
	}

	//
	// Integrate check results from the handlers
	//
	for (RuleType rule : ctx.getSelectedRules()) {
	    if (notApplicable.contains(rule.getId())) {
		RuleResultType rrt = Engine.FACTORY.createRuleResultType();
		rrt.setIdref(rule.getId());
		rrt.setWeight(rule.getWeight());
		rrt.setRole(rule.getRole());
		rrt.setResult(getRuleResult(rule, ResultEnumType.NOTAPPLICABLE));
		testResult.getRuleResult().add(rrt);
	    } else {
		testResult.getRuleResult().addAll(evaluate(rule, handlers));
	    }
	}

	testResult.setEndTime(getTimestamp());
	producer.sendNotify(Message.RULES_PHASE_END, null);

	//
	// Compute scores
	//
	HashMap<String, RuleResultType> resultMap = new HashMap<String, RuleResultType>();
	for (RuleResultType rrt : testResult.getRuleResult()) {
	    resultMap.put(rrt.getIdref(), rrt);
	}
	// Compute scores for all the supported models.
	// Note - only selected rules will be in the resultMap at this point
	BenchmarkType bt = ctx.getBenchmark().getBenchmark();
	for (ScoringModel model : ScoringModel.values()) {
	    try {
		ScoreKeeper sk = computeScore(resultMap, bt, model);
		ScoreType scoreType = FACTORY.createScoreType();
		scoreType.setSystem(model.uri());
		String score = Float.toString(sk.getScore());
		scoreType.setValue(new BigDecimal(score));
		scoreType.setMaximum(new BigDecimal(Float.toString(sk.getMaxScore())));
		testResult.getScore().add(scoreType);
		logger.info(JOVALMsg.STATUS_XCCDF_SCORE, model.uri(), score);
	    } catch (IllegalArgumentException e) {
		logger.warn(JOVALMsg.ERROR_XCCDF_MODEL, e.getMessage());
	    }
	}
    }

    /**
     * Evaluate the check(s) in the rule, and return the results.
     */
    private List<RuleResultType> evaluate(RuleType rule, Map<String, ISystem> handlers) throws Exception {
	List<RuleResultType> results = new ArrayList<RuleResultType>();
	if (rule.isSetComplexCheck()) {
	    ComplexCheckType check = rule.getComplexCheck();
	    ComplexCheckType checkResult = FACTORY.createComplexCheckType();
	    checkResult.setNegate(check.getNegate());
	    checkResult.setOperator(check.getOperator());

	    RuleResultType rrt = FACTORY.createRuleResultType();
	    rrt.setIdref(rule.getId());
	    rrt.setWeight(rule.getWeight());
	    rrt.setRole(rule.getRole());
	    rrt.setComplexCheck(checkResult);
	    rrt.setResult(getRuleResult(rule, evaluate(check, checkResult, handlers)));
	    results.add(rrt);
	} else {
	    for (CheckType check : rule.getCheck()) {
		if (handlers.containsKey(check.getSystem())) {
		    ISystem.IResult result = handlers.get(check.getSystem()).getResult(check, check.getMultiCheck());
		    switch(result.getType()) {
		      case SINGLE: {
			RuleResultType rrt = FACTORY.createRuleResultType();
			rrt.setIdref(rule.getId());
			rrt.setWeight(rule.getWeight());
			rrt.setRole(rule.getRole());
			rrt.getCheck().add(result.getCheck());
			rrt.setResult(getRuleResult(rule, result.getResult()));
			results.add(rrt);
			break;
		      }

		      case MULTI:
			for (ISystem.IResult subresult : result.getResults()) {
			    RuleResultType rrt = Engine.FACTORY.createRuleResultType();
			    rrt.setIdref(rule.getId());
			    rrt.setWeight(rule.getWeight());
			    rrt.setRole(rule.getRole());
			    rrt.getCheck().add(subresult.getCheck());
			    rrt.getInstance().add(subresult.getInstance());
			    rrt.setResult(getRuleResult(rule, subresult.getResult()));
			    results.add(rrt);
			}
			break;
		    }
		    break;
		}
	    }
	}
	return results;
    }

    /**
     * Recursively evaluate the complex check, adding processed check information to the checkResult, and return the
     * overall result.
     */
    private ResultEnumType evaluate(ComplexCheckType check, ComplexCheckType checkResult, Map<String, ISystem> handlers)
		throws Exception {

	CheckData data = new CheckData(check.getNegate());
	for (Object obj : check.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		CheckType subCheck = (CheckType)obj;
		if (handlers.containsKey(subCheck.getSystem())) {
		    ISystem.IResult result = handlers.get(subCheck.getSystem()).getResult(subCheck, false);
		    checkResult.getCheckOrComplexCheck().add(result.getCheck());
		    data.add(result.getResult());
		} else {
		    data.add(ResultEnumType.NOTCHECKED);
		}
	    } else if (obj instanceof ComplexCheckType) {
		ComplexCheckType subCheck = (ComplexCheckType)obj;
		ComplexCheckType subCheckResult = FACTORY.createComplexCheckType();
		subCheckResult.setNegate(subCheck.getNegate());
		subCheckResult.setOperator(subCheck.getOperator());
		checkResult.getCheckOrComplexCheck().add(subCheckResult);
		data.add(evaluate(subCheck, subCheckResult, handlers));
	    }
	}
	return data.getResult(check.getOperator());
    }

    /**
     * Change the ResultEnumType returned by check(s) as required by the rule.
     */
    private ResultEnumType getRuleResult(RuleType rule, ResultEnumType result) {
	RoleEnumType role = rule.getRole();
	switch(role) {
	  case UNSCORED:
	    return ResultEnumType.INFORMATIONAL;

	  case FULL:
	    return result;

	  default:
	    throw new IllegalArgumentException(role.toString());
	}
    }

    /**
     * Recursively add all the checks in the ComplexCheckType to the handlers.
     */
    private void addAllChecks(ComplexCheckType complex, Map<String, ISystem> handlers) throws Exception {
	for (Object obj : complex.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		CheckType check = (CheckType)obj;
		if (handlers.containsKey(check.getSystem())) {
		    handlers.get(check.getSystem()).add(check);
		}
	    } else if (obj instanceof ComplexCheckType) {
		addAllChecks((ComplexCheckType)obj, handlers);
	    }
	}
    }

    /**
     * Create a Benchmark.TestResult node, initialized with information gathered from the context and plugin.
     */
    private TestResultType initializeResult() throws OvalException {
	TestResultType testResult = FACTORY.createTestResultType();
	String id = ctx.getBenchmark().getId();
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
	testResult.setVersion(ctx.getBenchmark().getBenchmark().getVersion().getValue());
	testResult.setTestSystem(PRODUCT_NAME);

	TestResultType.Benchmark trb = FACTORY.createTestResultTypeBenchmark();
	trb.setId(ctx.getBenchmark().getBenchmark().getBenchmarkId());
	trb.setHref(ctx.getBenchmark().getHref());
	testResult.setBenchmark(trb);
	if (ctx.getProfile() != null) {
	    IdrefType profileRef = FACTORY.createIdrefType();
	    profileRef.setIdref(ctx.getProfile().getProfileId());
	    testResult.setProfile(profileRef);
	}
	String user = plugin.getSession().getUsername();
	if (user != null) {
	    IdentityType identity = FACTORY.createIdentityType();
	    identity.setValue(user);
	    if (IUnixSession.ROOT.equals(user) || IWindowsSession.ADMINISTRATOR.equalsIgnoreCase(user)) {
		identity.setPrivileged(true);
	    }
	    identity.setAuthenticated(true);
	    testResult.setIdentity(identity);
	}
	sysinfo = plugin.getSystemInfo();
	applicableCpes = new ArrayList<String>();
	if (!testResult.getTarget().contains(sysinfo.getPrimaryHostName())) {
	    testResult.getTarget().add(sysinfo.getPrimaryHostName());
	}
	for (InterfaceType intf : sysinfo.getInterfaces().getInterface()) {
	    if (!testResult.getTargetAddress().contains(intf.getIpAddress())) {
		testResult.getTargetAddress().add(intf.getIpAddress());
	    }
	}

	//
	// Add all the selected values to the results
	//
	for (Map.Entry<String, Collection<String>> entry : ctx.getValues().entrySet()) {
	    if (entry.getValue().size() == 1) {
		ProfileSetValueType val = FACTORY.createProfileSetValueType();
		val.setIdref(entry.getKey());
		val.setValue(entry.getValue().iterator().next());
		testResult.getSetValueOrSetComplexValue().add(val);
	    } else {
		ProfileSetComplexValueType val = FACTORY.createProfileSetComplexValueType();
		val.setIdref(entry.getKey());
		val.getItem().addAll(entry.getValue());
		testResult.getSetValueOrSetComplexValue().add(val);
	    }
	}

	testResult.setStartTime(getTimestamp());
	return testResult;
    }

    /**
     * Test the target against all the platforms defined in the context, and store the results indexed by CVE ID in the
     * platforms map.
     */
    private void checkPlatforms(TestResultType testResult) throws Exception {
	//
	// Parcel up all the check definitions for auto-discovery by href
	//
	Map<String, IDefinitionFilter> filters = new HashMap<String, IDefinitionFilter>();
	for (Map.Entry<String, LogicalTestType> entry : ctx.getCpeTests().entrySet()) {
	    if (platforms.containsKey(entry.getKey())) {
		// Ignore asserted platforms
	    } else {
		producer.sendNotify(Message.PLATFORM_CPE, entry.getKey());
		for (CheckFactRefType cfrt : entry.getValue().getCheckFactRef()) {
		    if (SystemEnumeration.OVAL.namespace().equals(cfrt.getSystem())) {
			if (!filters.containsKey(cfrt.getHref())) {
			    filters.put(cfrt.getHref(), OvalFactory.createDefinitionFilter());
			}
			filters.get(cfrt.getHref()).addDefinition(cfrt.getIdRef());
		    }
		}
	    }
	}

	//
	// Run an OVAL engine for each href to solve the platform definitions
	//
	Map<String, IResults> results = new HashMap<String, IResults>();
	for (Map.Entry<String, IDefinitionFilter> entry : filters.entrySet()) {
	    IOvalEngine engine = OvalFactory.createEngine(IOvalEngine.Mode.DIRECTED, plugin);
	    producer.sendNotify(Message.OVAL_ENGINE, engine);
	    DefinitionMonitor monitor = new DefinitionMonitor();
	    engine.getNotificationProducer().addObserver(monitor);
	    try {
		engine.setDefinitions(ctx.getOval(entry.getKey()));
		engine.setDefinitionFilter(entry.getValue());
		engine.run();
		switch(engine.getResult()) {
		  case OK:
		    IResults ir = engine.getResults();
		    results.put(entry.getKey(), ir);
		    //
		    // NB: Platform checks can make use of OVAL documents are also be leveraged by regular checks.
		    // In such cases, the platform OVAL subreport will be over-written by the OVAL results used
		    // to perform the XCCDF checks.
		    //
		    subreports.put(entry.getKey(), ir);
		    break;

		  case ERR:
		    throw engine.getError();
		}
	    } catch (NoSuchElementException e) {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, e.getMessage()));
	    } finally {
		engine.getNotificationProducer().removeObserver(monitor);
	    }
	}

	//
	// Evaluate the platform definitions based on assertions, logical tests and discovered data.
	//
	for (Map.Entry<String, LogicalTestType> entry : ctx.getCpeTests().entrySet()) {
	    if (platforms.containsKey(entry.getKey())) {
		if (platforms.get(entry.getKey()).booleanValue()) {
		    CPE2IdrefType cpeRef = FACTORY.createCPE2IdrefType();
		    cpeRef.setIdref(entry.getKey());
		    testResult.getPlatform().add(cpeRef);
		}
	    } else {
		if (evaluate(entry.getValue(), results)) {
		    CPE2IdrefType cpeRef = FACTORY.createCPE2IdrefType();
		    cpeRef.setIdref(entry.getKey());
		    testResult.getPlatform().add(cpeRef);
		    logger.info(JOVALMsg.STATUS_CPE_TARGET, plugin.getSession().getHostname(), entry.getKey());
		    platforms.put(entry.getKey(), Boolean.TRUE);
		} else {
		    platforms.put(entry.getKey(), Boolean.FALSE);
		}
	    }
	}
    }

    private boolean evaluate(LogicalTestType test, Map<String, IResults> results) throws Exception {
	OperatorData data = new OperatorData(test.isNegate());
	for (CheckFactRefType cfrt : test.getCheckFactRef()) {
	    data.addResult(results.get(cfrt.getHref()).getDefinitionResult(cfrt.getIdRef()));
	}
	OperatorEnumeration op;
	switch(test.getOperator()) {
	  case AND:
	    op = OperatorEnumeration.OR;
	    break;
	  case OR:
	    op = OperatorEnumeration.OR;
	    break;
	  default:
	    throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, test.getOperator().toString()));
	}
	ResultEnumeration result = data.getResult(op);
	switch(result) {
	  case TRUE:
	    return true;
	  case FALSE:
	    return false;
	  default:
	    logger.warn(JOVALMsg.WARNING_XCCDF_PLATFORM, result.toString());
	    return false;
	}
    }

    /**
     * Recursively get all of the rules within the XCCDF document.
     */
    private List<RuleType> listAllRules() {
	List<RuleType> rules = new ArrayList<RuleType>();
	for (SelectableItemType item : ctx.getBenchmark().getBenchmark().getGroupOrRule()) {
	    rules.addAll(getRules(item));
	}
	return rules;
    }

    /**
     * Recursively list all of the rules within the SelectableItem.
     */
    private List<RuleType> getRules(SelectableItemType item) {
	List<RuleType> rules = new ArrayList<RuleType>();
	if (item instanceof RuleType) {
	    rules.add((RuleType)item);
	} else if (item instanceof GroupType) {
	    for (SelectableItemType child : ((GroupType)item).getGroupOrRule()) {
		rules.addAll(getRules(child));
	    }
	}
	return rules;
    }

    /**
     * Get the current time as an XMLGregorianCalendar.
     */
    private XMLGregorianCalendar getTimestamp() {
	XMLGregorianCalendar tm = null;
	if (datatypeFactory != null) {
	    tm = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar());
	}
	return tm;
    }

    /**
     * Implementation of OcilMessageArgument.
     */
    static class Argument implements OcilMessageArgument {
	private String href;
	private IChecklist checklist;
	private Collection<String> questionnaireIds;
	private IVariables variables;

	Argument(String href, IChecklist checklist, Collection<String> questionnaireIds, IVariables variables) {
	    this.href = href;
	    this.checklist = checklist;
	    this.questionnaireIds = questionnaireIds;
	    this.variables = variables;
	}

	// Implement IXccdfEngine.OcilMessageArgument

	public String getHref() {
	    return href;
	}

	public IChecklist getChecklist() {
	    return checklist;
	}

	public Collection<String> getQuestionnaireIds() {
	    return questionnaireIds;
	}

	public IVariables getVariables() {
	    return variables;
	}
    }

    /**
     * An IObserver for an IOvalEngine, that tracks all the definitions that are evaluated.
     */
    class DefinitionMonitor implements IObserver<IOvalEngine.Message> {
	private HashSet<String> defs;

	DefinitionMonitor() {
	    defs = new HashSet<String>();
	}

	Collection<String> definitions() {
	    return defs;
	}

	public void notify(IProducer<IOvalEngine.Message> sender, IOvalEngine.Message msg, Object arg) {
	    switch(msg) {
	      case DEFINITION:
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

	String uri() {
	    return uri;
	}
    }

    /**
     * Recursively compute the score of a list of items.
     *
     * @param results a HashMap containing the results of all (i.e., exclusively) the /selected/ rules
     */
    ScoreKeeper computeScore(HashMap<String, RuleResultType> results, BenchmarkType bt, ScoringModel model)
		throws IllegalArgumentException {

	List<SelectableItemType> items = bt.getGroupOrRule();
	switch(model) {
	  case DEFAULT:
	    return new DefaultScoreKeeper(new DefaultScoreKeeper(results), items);
	  case FLAT:
	    return new FlatScoreKeeper(new FlatScoreKeeper(true, results), items);
	  case FLAT_UNWEIGHTED:
	    return new FlatScoreKeeper(new FlatScoreKeeper(false, results), items);
	  case ABSOLUTE:
	    return new AbsoluteScoreKeeper(new AbsoluteScoreKeeper(results), items);
	  default:
	    throw new IllegalArgumentException(model.toString());
	}
    }

    /**
     * The base class for ScoreKeepers.
     */
    abstract static class ScoreKeeper {
	HashMap<String, RuleResultType> results;
	float score, max_score, count;

	ScoreKeeper(HashMap<String, RuleResultType> results) {
	    this.results = results;
	    score = 0;
	    max_score = 0;
	    count = 0;
	}

	float getScore() {
	    return score;
	}

	float getMaxScore() {
	    return max_score;
	}
    }

    /**
     * ScoreKeeper implementation for ScoringModel.DEFAULT.
     */
    class DefaultScoreKeeper extends ScoreKeeper {
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
		weightedScore = ctx.getRule(rule.getId()).getWeight().intValue() * score;
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
		    RuleType rule = (RuleType)item;
		    child = new DefaultScoreKeeper(this, rule);
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

	@Override
	float getMaxScore() {
	    return 100;
	}
    }

    /**
     * ScoreKeeper implementation for ScoringModel.FLAT and FLAT_UNWEIGHTED.
     */
    class FlatScoreKeeper extends ScoreKeeper {
	private boolean weighted;

	FlatScoreKeeper(boolean weighted, HashMap<String, RuleResultType> results) {
	    super(results);
	    this.weighted = weighted;
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
		    int weight = ctx.getRule(rule.getId()).getWeight().intValue();
		    if (weighted) {
			max_score += weight;
			score = (weight * score / count);
		    } else {
			if (weight == 0) {
			    score = 0;
			} else {
			    max_score += count;
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
    }

    /**
     * ScoreKeeper implementation for ScoringModel.ABSOLUTE.
     */
    class AbsoluteScoreKeeper extends FlatScoreKeeper {
	AbsoluteScoreKeeper(HashMap<String, RuleResultType> results) {
	    super(true, results);
	}

	AbsoluteScoreKeeper(AbsoluteScoreKeeper parent, List<SelectableItemType> items) throws IllegalArgumentException {
	    super(parent, items);
	}

	@Override
	float getScore() {
	    if (super.getScore() == super.getMaxScore()) {
		return 1;
	    } else {
		return 0;
	    }
	}

	@Override
	float getMaxScore() {
	    return 1;
	}
    }

    private class AbortException extends RuntimeException {
	AbortException(String message) {
	    super(message);
	}
    }
}
