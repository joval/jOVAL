// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;

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

import org.joval.intf.oval.IResults;
import org.joval.oval.DefinitionFilter;
import org.joval.oval.Variables;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.engine.XPERT;

/**
 * XCCDF helper class for OVAL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OVALHandler {
    public static final String NAMESPACE = "http://oval.mitre.org/XMLSchema/oval-definitions-5";

    private XccdfBundle xccdf;
    private Profile profile;
    private DefinitionFilter filter;
    private Variables variables;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public OVALHandler(XccdfBundle xccdf, Profile profile) {
	this.xccdf = xccdf;
	this.profile = profile;
    }

    /**
     * Create an OVAL DefinitionFilter containing every selected rule in the profile with an OVAL check.
     */
    public DefinitionFilter getDefinitionFilter() {
	if (filter == null) {
	    filter = new DefinitionFilter();
	    Collection<RuleType> rules = profile.getSelectedRules();
	    for (RuleType rule : rules) {
		if (rule.isSetCheck()) {
		    for (CheckType check : rule.getCheck()) {
			if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.isSetName()) {
				    filter.addDefinition(ref.getName());
				}
			    }
			}
		    }
		}
	    }
	}
	return filter;
    }

    /**
     * Gather all the variable exports for OVAL checks from the selected rules in the profile, and create an OVAL
     * variables structure containing their values.
     */
    public Variables getVariables() {
	if (variables == null) {
	    variables = new Variables(XPERT.getGenerator());
	    Collection<RuleType> rules = profile.getSelectedRules();
	    Hashtable<String, String> values = profile.getValues();
	    for (RuleType rule : rules) {
		for (CheckType check : rule.getCheck()) {
		    if (check.getSystem().equals(NAMESPACE)) {
			for (CheckExportType export : check.getCheckExport()) {
			    String ovalVariableId = export.getExportName();
			    String valueId = export.getValueId();
			    variables.addValue(ovalVariableId, values.get(valueId));
			    variables.setComment(ovalVariableId, valueId);
			}
		    }
		}
	    }
	}
	return variables;
    }

    /**
     * Integrate the OVAL results with the XCCDF results, assuming the OVAL results contain information pertaining to
     * the selected rules in the profile.
     */
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

    // Internal

    /**
     * DAS: How should the results from multiple CheckContentRefTypes really be combined?
     */
    class RuleResult {
	private ResultEnumType result = ResultEnumType.NOTCHECKED;

	RuleResult() {}

	void add(ResultEnumType result) {
	    this.result = result;
	}

	ResultEnumType getResult() {
	    return result;
	}
    }
}
