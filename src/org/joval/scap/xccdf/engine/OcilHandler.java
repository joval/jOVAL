// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.io.File;
import java.util.ArrayList;
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
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.InstanceResultType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.RuleType;
import scap.xccdf.TestResultType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.ocil.IVariables;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.ocil.Variables;
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

    /**
     * Export relevant OCIL files to the specified directory. Returns false if there are no OCIL checks in the context.
     */
    public static boolean exportFiles(IScapContext ctx, Producer<IXccdfEngine.Message> producer) throws OcilException {
	Collection<String> hrefs = new HashSet<String>();
	Map<String, Variables> variables = new HashMap<String, Variables>();
	for (RuleType rule : ctx.getSelectedRules()) {
	    for (CheckType check : rule.getCheck()) {
		if (check.getSystem().equals(NAMESPACE)) {
		    if (check.isSetCheckContentRef()) {
			Variables vars = null;
			for (CheckContentRefType ref : check.getCheckContentRef()) {
			    hrefs.add(ref.getHref());
			    if (variables.containsKey(ref.getHref())) {
				vars = variables.get(ref.getHref());
			    } else {
				vars = new Variables();
				variables.put(ref.getHref(), vars);
			    }
			    for (CheckExportType export : check.getCheckExport()) {
				String ocilVariableId = export.getExportName();
				String valueId = export.getValueId();
				for (String s : ctx.getValues().get(valueId)) {
				    vars.addValue(ocilVariableId, s);
				}
				vars.setComment(ocilVariableId, valueId);
			    }
			}
		    }
		    break;
		}
	    }
	}

	if (hrefs.size() == 0) {
	    return false;
	}

	//
	// Export variables and OCIL XML for each HREF in the context.
	//
	for (String href : hrefs) {
	    try {
		IChecklist checklist = ctx.getOcil(href);
		IVariables vars = variables.get(href);
		producer.sendNotify(IXccdfEngine.Message.OCIL_MISSING, new Argument(href, checklist, vars));
	    } catch (NoSuchElementException e) {
		e.printStackTrace();
	    } catch (OcilException e) {
		e.printStackTrace();
	    }
	}
	return true;
    }

    private IScapContext ctx;
    private XMLGregorianCalendar startTime;
    private Map<String, Map<String, String>> results;
    private Collection<ITransformable> reports;

    /**
     * Create an OCIL handler utility for the given context and href-indexed Checklists (results).
     */
    public OcilHandler(IScapContext ctx, Map<String, IChecklist> checklists) throws IllegalArgumentException {
	this.ctx = ctx;
	results = new HashMap<String, Map<String, String>>();
	reports = new ArrayList<ITransformable>();

	if (checklists.size() == 1 && "".equals(checklists.keySet().iterator().next())) {
	    //
	    // If exactly one checklist was supplied, and without specifying an href, then treat is as a default checklist
	    // for all the OCIL hrefs in the context.
	    //
	    // For reporting purposes, however, it is only added once.
	    //
	    IChecklist checklist = checklists.values().iterator().next();
	    reports.add(checklist);

	    checklists = new HashMap<String, IChecklist>();
	    for (String href : getOcilHrefs()) {
		checklists.put(href, checklist);
	    }
	} else {
	    reports.addAll(checklists.values());
	}

	for (Map.Entry<String, IChecklist> entry : checklists.entrySet()) {
	    String href = entry.getKey();
	    IChecklist checklist = entry.getValue();
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

    public void add(CheckType check) {
	// No-op
    }

    public Collection<ITransformable> exec(IPlugin plugin) {
	return reports;
    }

    public IResult getResult(CheckType check, boolean multi) throws Exception {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
	if (check.isSetCheckContent()) {
	    // TBD (DAS): inline content is not supported
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
			data.add(ResultEnumType.NOTCHECKED);
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

    // Internal

    static class Argument implements IXccdfEngine.OcilMessageArgument {
	private String href;
	private IChecklist checklist;
	private IVariables variables;

	Argument(String href, IChecklist checklist, IVariables variables) {
	    this.href = href;
	    this.checklist = checklist;
	    this.variables = variables;
	}

	// Implement IXccdfEngine.OcilMessageArgument

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

    /**
     * Gather all the hrefs of all the OCIL check references.
     */
    private HashSet<String> getOcilHrefs() {
	HashSet<String> hrefs = new HashSet<String>();
	for (RuleType rule : ctx.getSelectedRules()) {
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
