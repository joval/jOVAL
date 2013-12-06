// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

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
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.InstanceResultType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.RuleType;
import scap.xccdf.TestResultType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;

/**
 * XCCDF helper class for OCIL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OcilHandler implements ISystem {
    public static final String NAMESPACE = SystemEnumeration.OCIL.namespace();

    private IScapContext ctx;
    private XMLGregorianCalendar startTime;
    private Map<String, Map<String, String>> results;
    private Map<String, IChecklist> reports;

    /**
     * Create an OCIL handler utility for the given context and href-indexed Checklists (results).
     */
    public OcilHandler(IScapContext ctx, Map<String, IChecklist> checklists) throws IllegalArgumentException {
	this.ctx = ctx;
	results = new HashMap<String, Map<String, String>>();
	reports = checklists;

	for (Map.Entry<String, IChecklist> entry : checklists.entrySet()) {
	    String href = entry.getKey();
	    IChecklist checklist = entry.getValue();
	    if (checklist.getRootObject().getValue().isSetResults()) {
		ResultsType rt = checklist.getRootObject().getValue().getResults();

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
     * Set the XCCDF test start time to the lesser of the OCIL start time, or the existing XCCDF start time.
     */
    public void setStartTime(TestResultType xccdfResult) {
	if (startTime != null) {
	    if (xccdfResult.isSetStartTime()) {
		if (startTime.toGregorianCalendar().compareTo(xccdfResult.getStartTime().toGregorianCalendar()) < 0) {
		    xccdfResult.setStartTime(startTime);
		}
	    } else {
		xccdfResult.setStartTime(startTime);
	    }
	}
    }

    // Implement ISystem

    public String getNamespace() {
	return NAMESPACE;
    }

    public void add(CheckType check) throws Exception {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
	if (check.isSetCheckContent()) {
	    throw new ScapException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCAP_CHECKCONTENT));
	}
    }

    public Map<String, ? extends ITransformable<?>> exec(IPlugin plugin) {
	return reports;
    }

    public IResult getResult(CheckType check, boolean multi) throws IllegalArgumentException {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    if (results.containsKey(ref.getHref())) {
		CheckData data = new CheckData(check.getNegate());
		Map<String, String> ocilResults = results.get(ref.getHref());
		if (ref.isSetName()) {
		    String name = ref.getName();
		    if (ocilResults.containsKey(name)) {
			data.add(convertResult(ocilResults.get(name)));
		    } else {
			data.add(ResultEnumType.UNKNOWN);
		    }
		} else if (multi) {
		    CheckResult cr = new CheckResult();
		    for (Map.Entry<String, String> entry : ocilResults.entrySet()) {
			data = new CheckData(check.getNegate());
			data.add(convertResult(entry.getValue()));
			InstanceResultType inst = Engine.FACTORY.createInstanceResultType();
			inst.setValue(entry.getKey());
			cr.getResults().add(new CheckResult(data.getResult(CcOperatorEnumType.AND), check, inst));
		    }
		    return cr;
		} else {
		    for (String qr : ocilResults.values()) {
			data.add(convertResult(qr));
		    }
		}
		return new CheckResult(data.getResult(CcOperatorEnumType.AND), check);
	    }
	}
	return new CheckResult(ResultEnumType.NOTCHECKED, check);
    }

    // Private

    /**
     * Convert a String questionnaire result into a check result.
     * @see NIST IR7692 Appendix B: Mapping OCIL Results to XCCDF Results
     */
    private ResultEnumType convertResult(String qr) {
	if ("PASS".equals(qr)) {
	    return ResultEnumType.PASS;
	} else if ("FAIL".equals(qr)) {
	    return ResultEnumType.FAIL;
	} else if (qr.equals(ExceptionalResultType.UNKNOWN.value())) {
	    return ResultEnumType.UNKNOWN;
	} else if (qr.equals(ExceptionalResultType.ERROR.value())) {
	    return ResultEnumType.ERROR;
	} else if (qr.equals(ExceptionalResultType.NOT_TESTED.value())) {
	    return ResultEnumType.NOTCHECKED;
	} else if (qr.equals(ExceptionalResultType.NOT_APPLICABLE.value())) {
	    return ResultEnumType.NOTAPPLICABLE;
	} else {
	    return ResultEnumType.UNKNOWN;
	}
    }
}
