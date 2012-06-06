// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

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
import xccdf.schemas.core.GroupType;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ProfileSetValueType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.SelectableItemType;
import xccdf.schemas.core.TestResultType;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.IVariables;
import org.joval.intf.system.IBaseSession;
import org.joval.oval.OvalException;
import org.joval.oval.OvalFactory;
import org.joval.xccdf.Benchmark;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.engine.XPERT;
import org.joval.xccdf.engine.RuleResult;

/**
 * XCCDF helper class for OVAL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OVALHandler {
    public static final String NAMESPACE = "http://oval.mitre.org/XMLSchema/oval-definitions-5";

    private Benchmark xccdf;
    private Profile profile;
    private ObjectFactory factory;
    private IVariables variables;
    private Hashtable<String, IEngine> engines;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile. An OVAL engine will be created for every
     * discrete OVAL href referenced by a profile-selected check in the XCCDF document.
     */
    public OVALHandler(Benchmark xccdf, Profile profile, IBaseSession session) throws Exception {
	this.xccdf = xccdf;
	this.profile = profile;
	factory = new ObjectFactory();
	engines = new Hashtable<String, IEngine>();
	for (RuleType rule : profile.getSelectedRules()) {
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref()) {
				String href = ref.getHref();
				if (!engines.containsKey(href)) {
				    session.getLogger().info("Creating engine for href " + href);
				    IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, session);
				    engine.setDefinitions(xccdf.getDefinitions(href));
				    engine.setExternalVariables(getVariables());
				    engine.setDefinitionFilter(getDefinitionFilter(href));
				    engines.put(href, engine);
				}
			    }
			}
		    }
		}
	    }
	}
    }

    /**
     * Get all the engines, to observe and run them.
     */
    public Collection<String> getHrefs() {
	return engines.keySet();
    }

    public IEngine getEngine(String href) {
	return engines.get(href);
    }

    /**
     * Integrate all the OVAL results with the XCCDF results.
     */
    public void integrateResults(TestResultType xccdfResult) throws OvalException {
	for (VariableType var : getVariables().getOvalVariables().getVariables().getVariable()) {
	    ProfileSetValueType val = factory.createProfileSetValueType();
	    val.setIdref(var.getComment());
	    if (var.isSetValue() && var.getValue().size() > 0 && var.getValue().get(0) != null) {
		val.setValue(var.getValue().get(0).toString());
		xccdfResult.getSetValueOrSetComplexValue().add(val);
	    }
	}
	for (String href : engines.keySet()) {
	    integrateResults(href, engines.get(href).getResults(), xccdfResult);
	}
    }

    // Private

    /**
     * Get all the definition IDs for a given Href based on the checks selected in the profile.
     */
    private IDefinitionFilter getDefinitionFilter(String href) {
	IDefinitionFilter filter = OvalFactory.createDefinitionFilter();
	for (RuleType rule : profile.getSelectedRules()) {
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref() && ref.getHref().equals(href)) {
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
    private IVariables getVariables() {
	if (variables == null) {
	    variables = OvalFactory.createVariables();
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
    private void integrateResults(String href, IResults ovalResult, TestResultType xccdfResult) throws OvalException {
	SystemInfoType info = ovalResult.getSystemCharacteristics().getSystemInfo();
	if (!xccdfResult.getTarget().contains(info.getPrimaryHostName())) {
	    xccdfResult.getTarget().add(info.getPrimaryHostName());
	}
	for (InterfaceType intf : info.getInterfaces().getInterface()) {
	    if (!xccdfResult.getTargetAddress().contains(intf.getIpAddress())) {
		xccdfResult.getTargetAddress().add(intf.getIpAddress());
	    }
	}

	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : profile.getSelectedRules()) {
	    RuleResultType ruleResult = factory.createRuleResultType();
	    String ruleId = rule.getId();
	    ruleResult.setIdref(ruleId);
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			ruleResult.getCheck().add(check);
			RuleResult result = new RuleResult();
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref() && ref.getHref().equals(href)) {
				if (ref.isSetName()) {
				    try {
					addResult(result, ovalResult.getDefinitionResult(ref.getName()));
				    } catch (NoSuchElementException e) {
					result.add(ResultEnumType.UNKNOWN);
				    }
				} else {
				    for (DefinitionType def : ovalResult.getDefinitionResults()) {
					addResult(result, def.getResult());
				    }
				}
				ruleResult.setResult(result.getResult());
				xccdfResult.getRuleResult().add(ruleResult);
				break;
			    }
			}
		    }
		}
	    }
	}
    }

    private void addResult(RuleResult rr, ResultEnumeration re) {
	switch (re) {
	  case ERROR:
	    rr.add(ResultEnumType.ERROR);
	    break;
    
	  case FALSE:
	    rr.add(ResultEnumType.FAIL);
	    break;
  
	  case TRUE:
	    rr.add(ResultEnumType.PASS);
	    break;
 
	  case UNKNOWN:
	  default:
	    rr.add(ResultEnumType.UNKNOWN);
	    break;
	}
    }
}
