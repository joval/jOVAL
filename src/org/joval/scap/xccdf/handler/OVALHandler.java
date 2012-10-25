// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.handler;

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
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.Profile;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.XPERT;
import org.joval.scap.xccdf.engine.RuleResult;

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
    private Hashtable<String, IVariables> variables;
    private Hashtable<String, IEngine> engines;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile. An OVAL engine will be created for every
     * discrete OVAL href referenced by a profile-selected check in the XCCDF document.
     */
    public OVALHandler(Benchmark xccdf, Profile profile, IBaseSession session) throws Exception {
	this.xccdf = xccdf;
	this.profile = profile;
	factory = new ObjectFactory();
	variables = new Hashtable<String, IVariables>();
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
				    engine.setExternalVariables(getVariables(href));
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
	for (String href : variables.keySet()) {
	    for (VariableType var : getVariables(href).getOvalVariables().getVariables().getVariable()) {
		ProfileSetValueType val = factory.createProfileSetValueType();
		val.setIdref(var.getComment());
		if (var.isSetValue() && var.getValue().size() > 0 && var.getValue().get(0) != null) {
		    val.setValue(var.getValue().get(0).toString());
		    xccdfResult.getSetValueOrSetComplexValue().add(val);
		}
	    }
	}
	for (String href : engines.keySet()) {
	    integrateResults(href, engines.get(href).getResults(), xccdfResult);
	}
    }

    /**
     * Get all the definition IDs for a given Href based on the checks selected in the profile.
     */
    public IDefinitionFilter getDefinitionFilter(String href) {
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
    public IVariables getVariables(String href) {
	if (!variables.containsKey(href)) {
	    IVariables vars = OvalFactory.createVariables();
	    variables.put(href, vars);
	    Collection<RuleType> rules = profile.getSelectedRules();
	    Hashtable<String, String> values = profile.getValues();
	    for (RuleType rule : rules) {
		for (CheckType check : rule.getCheck()) {
		    if (check.getSystem().equals(NAMESPACE)) {
			boolean applicable = false;
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (href.equals(ref.getHref())) {
				applicable = true;
				break;
			    }
			}
			if (applicable) {
			    for (CheckExportType export : check.getCheckExport()) {
				String ovalVariableId = export.getExportName();
				String valueId = export.getValueId();
				vars.addValue(ovalVariableId, values.get(valueId));
				vars.setComment(ovalVariableId, valueId);
			    }
			}
		    }
		}
	    }
	}
	return variables.get(href);
    }

    // Private

    /**
     * Integrate the OVAL results with the XCCDF results, assuming the OVAL results contain information pertaining to
     * the selected rules in the profile.
     */
    private void integrateResults(String href, IResults ovalResult, TestResultType xccdfResult) throws OvalException {
	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : profile.getSelectedRules()) {
	    RuleResultType ruleResult = factory.createRuleResultType();
	    String ruleId = rule.getId();
	    ruleResult.setIdref(ruleId);
	    ruleResult.setWeight(rule.getWeight());
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
