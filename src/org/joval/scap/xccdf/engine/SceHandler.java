// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import jsaf.intf.system.ISession;
import jsaf.intf.util.ILoggable;

import org.openscap.sce.xccdf.ScriptDataType;
import org.openscap.sce.results.SceResultsType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.ObjectFactory;
import scap.xccdf.OverrideableCPE2IdrefType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.TestResultType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.xccdf.IEngine;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.sce.SceException;
import org.joval.scap.sce.SCEScript;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.RuleResult;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;

/**
 * XCCDF helper class for SCE processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SceHandler implements ILoggable {
    public static final String NAMESPACE = SystemEnumeration.SCE.namespace();

    private IView view;
    private ISession session;
    private LocLogger logger;
    private Map<String, Map<String, Script>> scriptTable;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SceHandler(IView view, IPlugin plugin, Map<String, Boolean> platforms) {
	this.view = view;
	session = plugin.getSession();
	logger = plugin.getLogger();
	loadScripts(platforms);
    }

    public int ruleCount() {
	return scriptTable.size();
    }

    /**
     * Run all the SCE scripts and integrate the results with the XCCDF results in one step.
     */
    public void integrateResults(TestResultType xccdfResult, Producer<IEngine.Message> producer) {
	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : view.getSelectedRules()) {
	    String ruleId = rule.getId();
	    if (scriptTable.containsKey(ruleId)) {
		RuleResultType ruleResult = Engine.FACTORY.createRuleResultType();
		ruleResult.setIdref(ruleId);
		ruleResult.setWeight(rule.getWeight());
		if (rule.isSetCheck()) {
		    for (CheckType check : rule.getCheck()) {
			if (NAMESPACE.equals(check.getSystem()) && scriptTable.containsKey(ruleId)) {
			    RuleResult result = new RuleResult();
			    CheckType checkResult = Engine.FACTORY.createCheckType();

			    boolean importStdout = false;
			    for (CheckImportType cit : check.getCheckImport()) {
				if ("stdout".equals(cit.getImportName())) {
				    importStdout = true;
				    break;
				}
			    }

			    Map<String, Script> ruleScripts = scriptTable.get(ruleId);
			    if (check.isSetCheckContentRef()) {
				for (CheckContentRefType ref : check.getCheckContentRef()) {
				    checkResult.getCheckContentRef().add(ref);
				    if (ruleScripts.containsKey(ref.getHref())) {
					Script rs = ruleScripts.get(ref.getHref());
					try {
					    producer.sendNotify(IEngine.Message.SCE_SCRIPT, ref.getHref());
					    SceResultsType srt = new SCEScript(rs.getExports(), rs.getData(), session).exec();
					    result.add(srt.getResult());
					    if (importStdout) {
						CheckImportType cit = Engine.FACTORY.createCheckImportType();
						cit.setImportName("stdout");
						cit.getContent().add(srt.getStdout());
						checkResult.getCheckImport().add(cit);
					    }
					} catch (Exception e) {
					    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
					    result.add(ResultEnumType.ERROR);
					}
				    }
				}
			    }
			    if (check.isSetCheckExport()) {
				checkResult.getCheckExport().addAll(check.getCheckExport());
			    }

			    ruleResult.getCheck().add(checkResult);
			    ruleResult.setResult(result.getResult());
			    xccdfResult.getRuleResult().add(ruleResult);
			}
		    }
		}
	    }
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    /**
     * Create a list of SCE scripts that should be executed based on the view.
     */
    private void loadScripts(Map<String, Boolean> platforms) {
	scriptTable = new HashMap<String, Map<String, Script>>();
	for (RuleType rule : view.getSelectedRules()) {
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
	    if (platformCheck && rule.isSetCheck()) {
		String ruleId = rule.getId();
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref()) {
				String scriptId = ref.getHref();
				try {
				    Map<String, String> exports = new HashMap<String, String>();
				    for (CheckExportType export : check.getCheckExport()) {
					exports.put(export.getExportName(), getSingleValue(export.getValueId()));
				    }
				    if (!scriptTable.containsKey(ruleId)) {
					scriptTable.put(ruleId, new HashMap<String, Script>());
				    }
				    scriptTable.get(ruleId).put(scriptId, new Script(scriptId, exports));
				} catch (IllegalArgumentException e) {
				    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
				} catch (SceException e) {
				    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
				} catch (NoSuchElementException e) {
				    logger.warn(JOVALMsg.ERROR_XCCDF_MISSING_PART, scriptId);
				}
			    }
			}
		    }
		}
	    }
	}
    }

    private String getSingleValue(String id) throws SceException {
	Collection<String> values = view.getValues().get(id);
	if (values.size() == 1) {
	    return values.iterator().next();
	} else {
	    throw new SceException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_VARS, id, values.size()));
	}
    }

    class Script {
	String id;
	Map<String, String> exports;
	ScriptDataType data;

	Script(String id, Map<String, String> exports) throws NoSuchElementException, SceException {
	    this.id = id;
	    data = view.getStream().getSce(id);
	    this.exports = exports;
	}

	Map<String, String> getExports() {
	    return exports;
	}

	ScriptDataType getData() {
	    return data;
	}
    }
}
