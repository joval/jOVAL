// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.handler;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.datatype.XMLGregorianCalendar;

import scap.ocil.core.ExceptionalResultType;
import scap.ocil.core.ResultsType;
import scap.ocil.core.QuestionnaireResultType;
import scap.ocil.variables.OcilVariables;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.ObjectFactory;
import scap.xccdf.ResultEnumType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.TestResultType;

import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.ocil.IVariables;
import org.joval.intf.scap.xccdf.IEngine;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.ocil.Variables;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.RuleResult;
import org.joval.scap.xccdf.engine.XPERT;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;

/**
 * XCCDF helper class for OCIL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OCILHandler {
    public static final String NAMESPACE = SystemEnumeration.OCIL.namespace();

    private IView view;
    private XMLGregorianCalendar startTime;
    private Map<String, Map<String, String>> results;
    private Map<String, Variables> variables;
    private ObjectFactory factory;

    /**
     * Create an OCIL handler utility for the given XCCDF, Profile and href-indexed Checklists (results).
     */
    public OCILHandler(IView view, Map<String, IChecklist> checklists) throws IllegalArgumentException {
	this.view = view;
	factory = new ObjectFactory();
	results = new HashMap<String, Map<String, String>>();

	for (String href : checklists.keySet()) {
	    IChecklist checklist = checklists.get(href);

	    //
	    // If the HREF was not specified, then discover the singleton HREF, or throw an error.
	    //
	    if ("".equals(href)) {
		Collection<String> hrefs = getOcilHrefs();
		if (checklists.size() == 1 && hrefs.size() == 1) {
		    href = hrefs.iterator().next();
		} else {
		    throw new IllegalArgumentException("href");
		}
	    }

	    if (checklist.getOCILType().isSetResults()) {
		ResultsType rt = checklist.getOCILType().getResults();

		//
		// Set the start time to the earliest checklist result start time.
		//
		if (rt.isSetStartTime()) {
		    XMLGregorianCalendar cal = rt.getStartTime();
		    if (startTime == null) {
			startTime = cal;
		    } else if (cal.toGregorianCalendar().compareTo(startTime.toGregorianCalendar()) < 0) {
			startTime = cal;
		    }
		}

		if (rt.isSetQuestionnaireResults()) {
		    Map<String, String> result = new HashMap<String, String>();
		    for (QuestionnaireResultType qr : rt.getQuestionnaireResults().getQuestionnaireResult()) {
			result.put(qr.getQuestionnaireRef(), qr.getResult());
		    }
		    results.put(href, result);
		}
	    }
	}
    }

    /**
     * Create an OCIL handler utility for the given XCCDF, Profile and OCIL export directory.
     */
    public OCILHandler(IView view) {
	this.view = view;
	variables = new HashMap<String, Variables>();
    }

    /**
     * Export relevant OCIL files to the specified directory. Returns false if there are no OCIL checks in the view.
     */
    public boolean exportFiles(Producer producer) {
	HashSet<String> ocilHrefs = getOcilHrefs();
	if (ocilHrefs.size() == 0) {
	    //
	    // There are no OCIL files to export
	    //
	    return false;
	}

	//
	// Export variables and OCIL XML for each HREF in the view.
	//
	for (String href : ocilHrefs) {
	    try {
		IChecklist checklist = view.getStream().getOcil(href);
		IVariables variables = getVariables(href);
		producer.sendNotify(IEngine.MESSAGE_OCIL_MISSING, new Argument(href, checklist, variables));
	    } catch (NoSuchElementException e) {
		e.printStackTrace();
	    } catch (OcilException e) {
		e.printStackTrace();
	    }
	}
	return true;
    }

    /**
     * Integrate all the OCIL results with the XCCDF results.
     */
    public void integrateResults(TestResultType xccdfResult) {
	if (results == null) {
	    return;
	}

	//
	// Set the XCCDF test start time to the lesser of the OCIL start time, or the existing XCCDF start time.
	//
	if (startTime != null) {
	    if (xccdfResult.isSetStartTime()) {
		if (startTime.toGregorianCalendar().compareTo(xccdfResult.getStartTime().toGregorianCalendar()) < 0) {
		    xccdfResult.setStartTime(startTime);
		}
	    } else {
		xccdfResult.setStartTime(startTime);
	    }
	}

	//
	// Iterate through the rules and record the results
	//
	for (RuleType rule : view.getSelectedRules()) {
	    String ruleId = rule.getId();
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			if (check.isSetCheckContentRef()) {
			    RuleResultType ruleResult = factory.createRuleResultType();
			    ruleResult.setIdref(ruleId);
			    ruleResult.setWeight(rule.getWeight());
			    RuleResult result = new RuleResult();
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.isSetHref() && ref.isSetName()) {
				    String href = ref.getHref();
				    String name = ref.getName();
				    if (results.containsKey(href) && results.get(href).containsKey(name)) {
					String qr = results.get(href).get(name);
					if ("PASS".equals(qr)) {
					    result.add(ResultEnumType.PASS);
					} else if ("FAIL".equals(qr)) {
					    result.add(ResultEnumType.FAIL);
					} else if (qr.equals(ExceptionalResultType.UNKNOWN.value())) {
					    result.add(ResultEnumType.UNKNOWN);
					} else if (qr.equals(ExceptionalResultType.ERROR.value())) {
					    result.add(ResultEnumType.ERROR);
					} else if (qr.equals(ExceptionalResultType.NOT_TESTED.value())) {
					    result.add(ResultEnumType.NOTCHECKED);
					} else if (qr.equals(ExceptionalResultType.NOT_APPLICABLE.value())) {
					    result.add(ResultEnumType.NOTAPPLICABLE);
					}
				    } else {
					result.add(ResultEnumType.NOTCHECKED);
				    }
				}
			    }
			    ruleResult.getCheck().add(check);
			    ruleResult.setResult(result.getResult());
			    xccdfResult.getRuleResult().add(ruleResult);
			}
		    }
		}
	    }
	}
    }

    // Internal

    class Argument implements IEngine.OcilMessageArgument {
	private String href;
	private IChecklist checklist;
	private IVariables variables;

	Argument(String href, IChecklist checklist, IVariables variables) {
	    this.href = href;
	    this.checklist = checklist;
	    this.variables = variables;
	}

	// Implement IEngine.OcilMessageArgument

	public String getHref() {
	    return href;
	}

	public IChecklist getChecklist() {
	    return checklist;
	}

	public IVariables getVariables() {
	    return variables;
	}
    }

    // Private

    /**
     * Gather all the variable exports for OCIL checks for the specified href from the selected rules in the view,
     * and create an OCIL variables structure containing their values.
     */
    private Variables getVariables(String href) throws OcilException {
	if (!variables.containsKey(href)) {
	    Variables vars = new Variables();
	    Collection<RuleType> rules = view.getSelectedRules();
	    Map<String, String> values = view.getValues();
	    for (RuleType rule : rules) {
		for (CheckType check : rule.getCheck()) {
		    if (check.getSystem().equals(NAMESPACE)) {
			if (check.isSetCheckContentRef()) {
			    boolean match = false;
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.getHref().equals(href)) {
				    match = true;
				    break;
				}
			    }
			    if (match) {
				for (CheckExportType export : check.getCheckExport()) {
				    String ocilVariableId = export.getExportName();
				    String valueId = export.getValueId();
				    vars.addValue(ocilVariableId, values.get(valueId));
				    vars.setComment(ocilVariableId, valueId);
				}
			    }
			}
		    }
		}
	    }
	    variables.put(href, vars);
	}
	return variables.get(href);
    }

    /**
     * Gather all the hrefs of all the OCIL check references.
     */
    private HashSet<String> getOcilHrefs() {
	HashSet<String> hrefs = new HashSet<String>();
	for (RuleType rule : view.getSelectedRules()) {
	    for (CheckType check : rule.getCheck()) {
		if (NAMESPACE.equals(check.getSystem())) {
		    for (CheckContentRefType ref : check.getCheckContentRef()) {
			if (ref.isSetHref()) {
			    hrefs.add(ref.getHref());
			}
		    }
		}
	    }
	}
	return hrefs;
    }
}
