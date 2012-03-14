// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import oval.schemas.common.GeneratorType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.DefinitionType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.variables.core.VariableType;

import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.TestResultType;

import org.joval.sce.SCEScript;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
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
    private List<SCEScript> scripts;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SCEHandler(XccdfBundle xccdf, Profile profile) {
	this.xccdf = xccdf;
	this.profile = profile;
    }

    /**
     * Create a list of SCE scripts that should be executed based on the profile.
     */
    public List<SCEScript> getScripts() {
	if (scripts == null) {
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
					SCEScript script = new SCEScript(xccdf.getURL(ref.getHref()));
					for (CheckExportType export : check.getCheckExport()) {
					    String name = export.getExportName();
					    String valueId = export.getValueId();
					    script.setenv(name, values.get(valueId));
					}
					scripts.add(script);
				    } catch (MalformedURLException e) {
					xccdf.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
     * Integrate the OVAL results with the XCCDF results, assuming the OVAL results contain information pertaining to
     * the selected rules in the profile.
    public void integrateResults(IResults ovalResult, TestResultType xccdfResult) {
	ObjectFactory factory = new ObjectFactory();
	xccdfResult.getTarget().add(ovalResult.getSystemInfo().getPrimaryHostName());
	for (InterfaceType intf : ovalResult.getSystemInfo().getInterfaces().getInterface()) {
	    xccdfResult.getTargetAddress().add(intf.getIpAddress());
	}
	for (VariableType var : getVariables().getOvalVariables().getVariables().getVariable()) {
	    ProfileSetValueType val = factory.createProfileSetValueType();
	    val.setIdref(var.getComment());
	    val.setValue(var.getValue().get(0).toString());
	    xccdfResult.getSetValue().add(val);
	}

	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : profile.getSelectedRules()) {
	    RuleResultType ruleResult = factory.createRuleResultType();
	    String ruleId = rule.getItemId();
	    ruleResult.setIdref(ruleId);
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			ruleResult.getCheck().add(check);

			RuleResult result = new RuleResult();
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetName()) {
				try {
				    switch (ovalResult.getDefinitionResult(ref.getName())) {
				      case ERROR:
					result.add(ResultEnumType.ERROR);
					break;
    
				      case FALSE:
					result.add(ResultEnumType.FAIL);
					break;
    
				      case TRUE:
					result.add(ResultEnumType.PASS);
					break;
    
				      case UNKNOWN:
				      default:
					result.add(ResultEnumType.UNKNOWN);
					break;
				    }
				} catch (NoSuchElementException e) {
				    result.add(ResultEnumType.NOTCHECKED);
				}
				ruleResult.setResult(result.getResult());
			    }
			}
			xccdfResult.getRuleResult().add(ruleResult);
			break;
		    }
		}
	    }
	}
    }
     */
}
