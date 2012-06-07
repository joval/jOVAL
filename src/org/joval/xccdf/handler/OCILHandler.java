// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.handler;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.datatype.XMLGregorianCalendar;

import ocil.schemas.core.ExceptionalResultType;
import ocil.schemas.core.ResultsType;
import ocil.schemas.core.QuestionnaireResultType;
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
    private Hashtable<String, QuestionnaireResultType> questionnaireResults;
    private ObjectFactory factory;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public OCILHandler(Benchmark xccdf, Profile profile, Checklist checklist) {
	this.xccdf = xccdf;
	this.profile = profile;
	if (checklist.getOCILType().isSetResults()) {
	    questionnaireResults = new Hashtable<String, QuestionnaireResultType>();
	    factory = new ObjectFactory();
	    ResultsType results = checklist.getOCILType().getResults();
	    if (results.isSetStartTime()) {
		startTime = results.getStartTime();
	    }
	    if (results.isSetQuestionnaireResults() && results.getQuestionnaireResults().isSetQuestionnaireResult()) {
		for (QuestionnaireResultType qr : results.getQuestionnaireResults().getQuestionnaireResult()) {
		    questionnaireResults.put(qr.getQuestionnaireRef(), qr);
		}
	    }
	}
    }

    /**
     * Integrate all the OCIL results with the XCCDF results.
     */
    public void integrateResults(TestResultType xccdfResult) {
	if (questionnaireResults == null) {
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
				checkResult.getCheckContentRef().add(ref);
				if (questionnaireResults.containsKey(ref.getName())) {
				    String qr = questionnaireResults.get(ref.getName()).getResult();
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
