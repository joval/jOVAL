// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.handler;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.oval.common.GeneratorType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.DefinitionType;
import scap.oval.variables.VariableType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckType;
import scap.xccdf.CheckExportType;
import scap.xccdf.GroupType;
import scap.xccdf.InstanceResultType;
import scap.xccdf.ObjectFactory;
import scap.xccdf.ProfileSetValueType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.IVariables;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.plugin.IPlugin;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.RuleResult;

/**
 * XCCDF helper class for OVAL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OVALHandler {
    public static final String NAMESPACE = SystemEnumeration.OVAL.namespace();

    private IView view;
    private ObjectFactory factory;
    private Map<String, IVariables> variables;
    private Map<String, IEngine> engines;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile. An OVAL engine will be created for every
     * discrete OVAL href referenced by a profile-selected check in the XCCDF document.
     */
    public OVALHandler(IView view, IPlugin plugin) throws Exception {
	this.view = view;
	factory = new ObjectFactory();
	variables = new HashMap<String, IVariables>();
	engines = new HashMap<String, IEngine>();
	for (RuleType rule : view.getSelectedRules()) {
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref()) {
				String href = ref.getHref();
				if (!engines.containsKey(href)) {
				    plugin.getLogger().info("Creating engine for href " + href);
				    IEngine engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, plugin);
				    engine.setDefinitions(view.getStream().getOval(href));
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
     * Get all the definition IDs for a given Href based on the checks selected in the view.
     */
    public IDefinitionFilter getDefinitionFilter(String href) {
	IDefinitionFilter filter = OvalFactory.createDefinitionFilter();
	for (RuleType rule : view.getSelectedRules()) {
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
     * Gather all the variable exports for OVAL checks from the selected rules in the view, and create an OVAL
     * variables structure containing their values.
     */
    public IVariables getVariables(String href) {
	if (!variables.containsKey(href)) {
	    IVariables vars = OvalFactory.createVariables();
	    variables.put(href, vars);
	    Collection<RuleType> rules = view.getSelectedRules();
	    Map<String, String> values = view.getValues();
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
     * the selected rules in the view.
     */
    private void integrateResults(String href, IResults ovalResult, TestResultType xccdfResult) throws OvalException {
	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : view.getSelectedRules()) {
	    String ruleId = rule.getId();
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			RuleResultType ruleResult = factory.createRuleResultType();
			ruleResult.setIdref(ruleId);
			ruleResult.setWeight(rule.getWeight());
			ruleResult.getCheck().add(check);
			RuleResult result = new RuleResult();
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref() && ref.getHref().equals(href)) {
				if (ref.isSetName()) {
				    try {
					result.add(convertResult(ovalResult.getDefinitionResult(ref.getName())));
				    } catch (NoSuchElementException e) {
					result.add(ResultEnumType.UNKNOWN);
				    }
				    ruleResult.setResult(result.getResult());
				    xccdfResult.getRuleResult().add(ruleResult);
				} else if (check.isSetMultiCheck() && check.getMultiCheck()) {
				    //
				    // @multicheck=true means a rule-result for each contained OVAL result
				    //
				    for (DefinitionType def : ovalResult.getDefinitionResults()) {
					RuleResultType rrt = factory.createRuleResultType();
					rrt.setIdref(ruleId);
					rrt.setWeight(rule.getWeight());
					rrt.getCheck().add(check);
					rrt.setResult(convertResult(def.getResult()));
					InstanceResultType inst = factory.createInstanceResultType();
					inst.setValue(def.getDefinitionId());
					rrt.getInstance().add(inst);
					xccdfResult.getRuleResult().add(rrt);
				    }
				} else {
				    for (DefinitionType def : ovalResult.getDefinitionResults()) {
					result.add(convertResult(def.getResult()));
				    }
				    ruleResult.setResult(result.getResult());
				    xccdfResult.getRuleResult().add(ruleResult);
				}
				break;
			    }
			}
		    }
		}
	    }
	}
    }

    /**
     * Map an OVAL result to an XCCDF result.
     */
    private ResultEnumType convertResult(ResultEnumeration re) {
	switch (re) {
	  case ERROR:
	    return ResultEnumType.ERROR;
    
	  case FALSE:
	    return ResultEnumType.FAIL;
  
	  case TRUE:
	    return ResultEnumType.PASS;
 
	  case UNKNOWN:
	  default:
	    return ResultEnumType.UNKNOWN;
	}
    }
}
