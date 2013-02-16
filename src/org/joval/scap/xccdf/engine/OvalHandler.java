// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import scap.xccdf.OverrideableCPE2IdrefType;
import scap.xccdf.ProfileSetValueType;
import scap.xccdf.RoleEnumType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
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
public class OvalHandler {
    public static final String NAMESPACE = SystemEnumeration.OVAL.namespace();

    private IView view;
    private Map<String, EngineData> engines;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile. An OVAL engine will be created for every
     * discrete OVAL href referenced by a profile-selected check in the XCCDF document.
     */
    public OvalHandler(IView view, IPlugin plugin, Map<String, Boolean> platforms) throws Exception {
	this.view = view;

	engines = new HashMap<String, EngineData>();
	for (RuleType rule : view.getSelectedRules().values()) {
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
	    if (platformCheck && rule.isSetCheck() && rule.getRole() != RoleEnumType.UNCHECKED) {
		//
		// For each checkable rule, collect the data required to build the OVAL engine that will process its
		// referenced content.
		//
		for (CheckType check : rule.getCheck()) {
		    if (check.isSetSystem() && check.getSystem().equals(NAMESPACE)) {
			if (check.isSetCheckContent()) {
			    // TBD (DAS): inline content is not supported
			}

			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref()) {
				String href = ref.getHref();
				EngineData ed = null;
				if (engines.containsKey(href)) {
				    ed = engines.get(href);
				} else {
				    ed = new EngineData(view.getStream().getOval(href));
				    engines.put(href, ed);
				}

				//
				// Add definition references to the filter
				//
				if (ref.isSetName()) {
				    ed.getFilter().addDefinition(ref.getName());
				} else {
				    //
				    // Add all the definitions
				    //
				    IDefinitions definitions = view.getStream().getOval(href);
				    for (scap.oval.definitions.core.DefinitionType definition :
					 definitions.getOvalDefinitions().getDefinitions().getDefinition()) {
					ed.getFilter().addDefinition(definition.getId());
				    }
				}

				//
				// Add variable exports to the variables
				//
				for (CheckExportType export : check.getCheckExport()) {
				    String ovalVariableId = export.getExportName();
				    String valueId = export.getValueId();
				    for (String s : view.getValues().get(valueId)) {
					ed.getVariables().addValue(ovalVariableId, s);
				    }
				    ed.getVariables().setComment(ovalVariableId, valueId);
				}
			    }
			}
			break;
		    }
		}
	    }
	}

	//
	// Create an OVAL engine to process the selected checks for each OVAL document href
	//
	Iterator<Map.Entry<String, EngineData>> iter = engines.entrySet().iterator();
	while(iter.hasNext()) {
	    Map.Entry<String, EngineData> entry = iter.next();
	    if (entry.getValue().createEngine(plugin)) {
		plugin.getLogger().info("Created engine for href " + entry.getKey());
	    } else {
		plugin.getLogger().info("No engine created for href " + entry.getKey());
		iter.remove();
	    }
	}
    }

    /**
     * Get all the engines, to observe and run them.
     */
    public Collection<String> getHrefs() {
	return engines.keySet();
    }

    /**
     * Return the engine used to process the document href.
     */
    public IEngine getEngine(String href) throws NoSuchElementException {
	if (engines.containsKey(href)) {
	    return engines.get(href).getEngine();
	} else {
	    throw new NoSuchElementException(href);
	}
    }

    /**
     * Integrate all the OVAL results with the XCCDF results.
     */
    public void integrateResults(TestResultType xccdfResult) throws OvalException {
	for (String href : engines.keySet()) {
	    for (VariableType var : engines.get(href).getVariables().getOvalVariables().getVariables().getVariable()) {
		ProfileSetValueType val = Engine.FACTORY.createProfileSetValueType();
		val.setIdref(var.getComment());
		if (var.isSetValue() && var.getValue().size() > 0 && var.getValue().get(0) != null) {
		    val.setValue(var.getValue().get(0).toString());
		    xccdfResult.getSetValueOrSetComplexValue().add(val);
		}
	    }
	    integrateResults(href, engines.get(href).getEngine().getResults(), xccdfResult);
	}
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
	for (RuleType rule : view.getSelectedRules().values()) {
	    String ruleId = rule.getId();
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			RuleResultType ruleResult = Engine.FACTORY.createRuleResultType();
			ruleResult.setIdref(ruleId);
			ruleResult.setWeight(rule.getWeight());
			ruleResult.getCheck().add(check);
			RuleResult result = new RuleResult(check.getNegate());
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    if (ref.isSetHref() && ref.getHref().equals(href)) {
				if (ref.isSetName()) {
				    try {
					result.add(convertResult(ovalResult.getDefinitionResult(ref.getName())));
				    } catch (NoSuchElementException e) {
					result.add(ResultEnumType.UNKNOWN);
				    }
				    ruleResult.setResult(getResult(rule.getRole(), result.getResult()));
				    xccdfResult.getRuleResult().add(ruleResult);
				} else if (check.isSetMultiCheck() && check.getMultiCheck()) {
				    //
				    // @multicheck=true means a rule-result for each contained OVAL result
				    //
				    for (DefinitionType def : ovalResult.getDefinitionResults()) {
					RuleResultType rrt = Engine.FACTORY.createRuleResultType();
					rrt.setIdref(ruleId);
					rrt.setWeight(rule.getWeight());
					rrt.getCheck().add(check);
					rrt.setResult(getResult(rule.getRole(), convertResult(def.getResult())));
					InstanceResultType inst = Engine.FACTORY.createInstanceResultType();
					inst.setValue(def.getDefinitionId());
					rrt.getInstance().add(inst);
					xccdfResult.getRuleResult().add(rrt);
				    }
				} else {
				    for (DefinitionType def : ovalResult.getDefinitionResults()) {
					result.add(convertResult(def.getResult()));
				    }
				    ruleResult.setResult(getResult(rule.getRole(), result.getResult()));
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

    private ResultEnumType getResult(RoleEnumType role, ResultEnumType result) {
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

    class EngineData {
	private IDefinitions definitions;
	private IDefinitionFilter filter;
	private IVariables variables;
	private IEngine engine;

	EngineData(IDefinitions definitions) {
	    this.definitions = definitions;
	    filter = OvalFactory.createDefinitionFilter();
	    variables = OvalFactory.createVariables();
	}

	IDefinitionFilter getFilter() {
	    return filter;
	}

	IVariables getVariables() {
	    return variables;
	}

	boolean createEngine(IPlugin plugin) {
	    if (filter.size() > 0) {
		engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, plugin);
		engine.setDefinitions(definitions);
		engine.setExternalVariables(variables);
		engine.setDefinitionFilter(filter);
		return true;
	    } else {
		return false;
	    }
	}

	IEngine getEngine() {
	    return engine;
	}
    }
}
