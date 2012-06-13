// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.datatype.XMLGregorianCalendar;

import ocil.schemas.core.ExceptionalResultType;
import ocil.schemas.core.ResultsType;
import ocil.schemas.core.QuestionnaireResultType;
import ocil.schemas.variables.OcilVariables;
import xccdf.schemas.core.CheckContentRefType;
import xccdf.schemas.core.CheckExportType;
import xccdf.schemas.core.CheckImportType;
import xccdf.schemas.core.CheckType;
import xccdf.schemas.core.ObjectFactory;
import xccdf.schemas.core.ResultEnumType;
import xccdf.schemas.core.RuleResultType;
import xccdf.schemas.core.RuleType;
import xccdf.schemas.core.TestResultType;

import org.joval.ocil.Checklist;
import org.joval.ocil.OcilException;
import org.joval.ocil.Variables;
import org.joval.util.JOVALMsg;
import org.joval.xccdf.Benchmark;
import org.joval.xccdf.Profile;
import org.joval.xccdf.XccdfException;
import org.joval.xccdf.engine.RuleResult;
import org.joval.xccdf.engine.XPERT;

/**
 * XCCDF helper class for OCIL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OCILHandler {
    public static final String NAMESPACE = "http://scap.nist.gov/schema/ocil/2";

    private Benchmark xccdf;
    private Profile profile;
    private XMLGregorianCalendar startTime;
    private Hashtable<String, Hashtable<String, String>> results;
    private Hashtable<String, Variables> variables;
    private ObjectFactory factory;

    /**
     * Create an OCIL handler utility for the given XCCDF, Profile and href-indexed Checklists (results).
     */
    public OCILHandler(Benchmark xccdf, Profile profile, Hashtable<String, Checklist> checklists)
		throws IllegalArgumentException {

	this.xccdf = xccdf;
	this.profile = profile;
	factory = new ObjectFactory();
	results = new Hashtable<String, Hashtable<String, String>>();

	for (String href : checklists.keySet()) {
	    Checklist checklist = checklists.get(href);

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
		    Hashtable<String, String> result = new Hashtable<String, String>();
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
    public OCILHandler(Benchmark xccdf, Profile profile) {
	this.xccdf = xccdf;
	this.profile = profile;
	variables = new Hashtable<String, Variables>();
    }

    /**
     * Export relevant OCIL files to the specified directory. Returns false if there are no OCIL checks in the profile.
     */
    public boolean exportFiles(File exportDir) {
	HashSet<String> ocilHrefs = getOcilHrefs();
	if (ocilHrefs.size() == 0) {
	    //
	    // There are no OCIL files to export
	    //
	    return false;
	} else if (!exportDir.exists()) {
	    exportDir.mkdirs();
	}

	//
	// Export variables and OCIL XML for each HREF in the profile.
	//
	for (String href : ocilHrefs) {
	    try {
		Checklist c = xccdf.getChecklist(href);
		Variables vars = getVariables(href);
		String fbase = href;
		if (fbase.toLowerCase().endsWith(".xml")) {
		    fbase = fbase.substring(0, fbase.length() - 4);
		}
		if (vars.getOcilVariables().getVariables().getVariable().size() > 0) {
		    vars.writeXML(new File(exportDir, fbase + "-variables.xml"));
		}
		c.writeXML(new File(exportDir, fbase + ".xml"));
	    } catch (NoSuchElementException e) {
		e.printStackTrace();
	    } catch (IOException e) {
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
	for (RuleType rule : profile.getSelectedRules()) {
	    String ruleId = rule.getId();
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    if (NAMESPACE.equals(check.getSystem())) {
			if (check.isSetCheckContentRef()) {
			    RuleResultType ruleResult = factory.createRuleResultType();
			    ruleResult.setIdref(ruleId);
			    RuleResult result = new RuleResult();
			    CheckType checkResult = factory.createCheckType();
			    for (CheckContentRefType ref : check.getCheckContentRef()) {
				if (ref.isSetHref() && ref.isSetName()) {
				    String href = ref.getHref();
				    String name = ref.getName();
				    checkResult.getCheckContentRef().add(ref);
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
			    ruleResult.getCheck().add(checkResult);
			    ruleResult.setResult(result.getResult());
			    xccdfResult.getRuleResult().add(ruleResult);
			}
		    }
		}
	    }
	}
    }

    // Private

    /**
     * Gather all the variable exports for OCIL checks for the specified href from the selected rules in the profile,
     * and create an OCIL variables structure containing their values.
     */
    private Variables getVariables(String href) throws OcilException {
	if (!variables.containsKey(href)) {
	    Variables vars = new Variables();
	    Collection<RuleType> rules = profile.getSelectedRules();
	    Hashtable<String, String> values = profile.getValues();
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
	for (RuleType rule : profile.getSelectedRules()) {
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
