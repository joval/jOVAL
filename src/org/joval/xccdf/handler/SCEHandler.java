// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.CheckImportType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.TestResultType;

import org.joval.intf.system.ISession;
import org.joval.sce.SCEScript;
import org.joval.util.JOVALMsg;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.engine.RuleResult;
import org.joval.xccdf.engine.XPERT;

/**
 * XCCDF helper class for SCE processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SCEHandler {
    public static final String NAMESPACE = "http://open-scap.org/page/SCE";

    private XccdfBundle xccdf;
    private Profile profile;
    private ISession session;
    private Hashtable<String, Hashtable<String, SCEScript>> scriptTable;
    private List<SCEScript> scripts;
    private ObjectFactory factory;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SCEHandler(XccdfBundle xccdf, Profile profile, ISession session) {
	this.xccdf = xccdf;
	this.profile = profile;
	this.session = session;
	factory = new ObjectFactory();
    }

    /**
     * Create a list of SCE scripts that should be executed based on the profile.
     */
    public List<SCEScript> getScripts() {
	if (scriptTable == null) {
	    scriptTable = new Hashtable<String, Hashtable<String, SCEScript>>();
	    scripts = new Vector<SCEScript>();
	    Hashtable<String, String> values = profile.getValues();
	    Collection<RuleType> rules = profile.getSelectedRules();
	    for (RuleType rule : rules) {
		if (rule.isSetCheck()) {
		    for (CheckType check : rule.getCheck()) {
			if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.isSetHref()) {
				    try {
					String ruleId = rule.getItemId();
					SCEScript sce = new SCEScript(xccdf.getURL(ref.getHref()), session);
					for (CheckExportType export : check.getCheckExport()) {
					    String name = export.getExportName();
					    String valueId = export.getValueId();
					    sce.setExport(name, values.get(valueId));
					}
					if (!scriptTable.containsKey(ruleId)) {
					    scriptTable.put(ruleId, new Hashtable<String, SCEScript>());
					}
					Hashtable<String, SCEScript> table = scriptTable.get(ruleId);
					table.put(ref.getHref(), sce);
					scripts.add(sce);
				    } catch (MalformedURLException e) {
					xccdf.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	return scripts;
    }

    /**
     * Integrate all the SCE results with the XCCDF results.
     */
    public void integrateResults(TestResultType xccdfResult) {
	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : profile.getSelectedRules()) {
	    String ruleId = rule.getItemId();
	    if (scriptTable.containsKey(ruleId)) {
		RuleResultType ruleResult = factory.createRuleResultType();
		ruleResult.setIdref(ruleId);
		if (rule.isSetCheck()) {
		    for (CheckType check : rule.getCheck()) {
			if (NAMESPACE.equals(check.getSystem()) && scriptTable.containsKey(ruleId)) {
			    RuleResult result = new RuleResult();
			    CheckType checkResult = factory.createCheckType();

			    boolean importStdout = false;
			    for (CheckImportType cit : check.getCheckImport()) {
				if ("stdout".equals(cit.getImportName())) {
				    importStdout = true;
				    break;
				}
			    }

			    Hashtable<String, SCEScript> ruleScripts = scriptTable.get(ruleId);
			    if (check.isSetCheckContentRef()) {
				for (CheckContentRefType ref : check.getCheckContentRef()) {
				    checkResult.getCheckContentRef().add(ref);
				    if (ruleScripts.containsKey(ref.getHref())) {
					SCEScript script = ruleScripts.get(ref.getHref());
					result.add(script.getResult());
					if (importStdout) {
					    CheckImportType cit = factory.createCheckImportType();
					    cit.setImportName("stdout");
					    StringBuffer sb = new StringBuffer();
					    for (String line : script.getStdout()) {
						if (sb.length() > 0) {
						    sb.append("\n");
						}
						sb.append(line);
					    }
					    cit.setValue(sb.toString());
					    checkResult.getCheckImport().add(cit);
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
}
