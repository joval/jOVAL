// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.openscap.sce.results.SceResultsType;
import scap.ocil.core.OCILType;
import scap.xccdf.CheckType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.ComplexCheckType;
import scap.xccdf.RuleType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IDatastreamCollection;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.ocil.IExport;
import org.joval.intf.scap.ocil.IVariables;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ScapException;
import org.joval.scap.arf.ArfException;
import org.joval.scap.arf.Report;
import org.joval.scap.datastream.DatastreamCollection;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.ocil.Variables;
import org.joval.scap.sce.Result;
import org.joval.scap.xccdf.Bundle;
import org.joval.scap.xccdf.Tailoring;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.Engine;

/**
 * A convenience class for obtaining jOVAL's SCAP management objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ScapFactory {
    /**
     * A JAXB object factory for XCCDF data model elements.
     */
    public static final scap.xccdf.ObjectFactory XCCDF = new scap.xccdf.ObjectFactory();

    /**
     * A JAXB object factory for SCE results data model elements.
     */
    public static final org.openscap.sce.results.ObjectFactory SCE_RESULTS = new org.openscap.sce.results.ObjectFactory();

    /**
     * An empty checklist.
     */
    public static final IChecklist EMPTY_CHECKLIST = Checklist.EMPTY;

    /**
     * Create a datastream collection from a URL.
     */
    public static IDatastreamCollection createDatastreamCollection(URL url) throws ScapException {
	try {
	    return new DatastreamCollection(DatastreamCollection.getDSCollection(url.openStream()));
	} catch (IOException e) {
	    throw new ScapException(e);
	}
    }

    /**
     * Create a datastream collection from a file.
     */
    public static IDatastreamCollection createDatastreamCollection(File f) throws ScapException {
	return new DatastreamCollection(DatastreamCollection.getDSCollection(f));
    }

    /**
     * Create a datastream from an SCAP "bundle" of files (directory or ZIP file).
     */
    public static IDatastream createBundle(File f) throws ScapException {
	return new Bundle(f);
    }

    /**
     * Create an XCCDF tailoring from a file.
     */
    public static ITailoring createTailoring(File f) throws XccdfException {
	return new Tailoring(Tailoring.getTailoringType(f));
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a file.
     */
    public static IChecklist createChecklist(File f) throws OcilException {
	return new Checklist(f);
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a stream.
     */
    public static IChecklist createChecklist(InputStream in) throws OcilException {
	return new Checklist(in);
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a stream.
     */
    public static IChecklist createChecklist(OCILType ocil) throws OcilException {
	return new Checklist(ocil);
    }

    /**
     * Create an SCE script result from a data model object.
     */
    public static IScriptResult createResult(SceResultsType srt) {
	return new Result(srt);
    }

    /**
     * Create an ARF report from a stream.
     */
    public static IReport createReport(InputStream in) throws ArfException {
	return new Report(Report.getAssetReportCollection(in));
    }

    /**
     * Create an ARF report from a file.
     */
    public static IReport createReport(File f) throws ArfException {
	return new Report(Report.getAssetReportCollection(f));
    }

    /**
     * Discover all the OCIL exports (hrefs, variables and checklists) for the given context.
     */
    public static Collection<IExport> getOcilExports(IScapContext ctx) throws OcilException {
	Map<String, List<String>> qMap = new HashMap<String, List<String>>();
	Map<String, Variables> vMap = new HashMap<String, Variables>();
	for (RuleType rule : ctx.getSelectedRules()) {
	    if (rule.isSetCheck()) {
		for (CheckType check : rule.getCheck()) {
		    addExports(check, ctx, qMap, vMap);
		}
	    } else if (rule.isSetComplexCheck()) {
		addExports(rule.getComplexCheck(), ctx, qMap, vMap);
	    }
	}

	//
	// Export variables and OCIL XML for each HREF in the context.
	//
	Collection<IExport> results = new ArrayList<IExport>();
	for (Map.Entry<String, List<String>> entry : qMap.entrySet()) {
	    String href = entry.getKey();
	    try {
		IChecklist checklist = ctx.getOcil(href);
		IVariables vars = vMap.get(href);
		results.add(new OcilExport(href, checklist, entry.getValue(), vars));
	    } catch (NoSuchElementException e) {
		throw new OcilException(e);
	    }
	}
	return results;
    }

    /**
     * Create an XCCDF processing engine for all supported System types.
     */
    public static IXccdfEngine createEngine(IPlugin plugin) {
	return new OEMEngine(plugin, SystemEnumeration.ANY);
    }

    /**
     * Create an XCCDF processing engine for the specified System types.
     */
    public static IXccdfEngine createEngine(IPlugin plugin, SystemEnumeration... systems) {
	return new OEMEngine(plugin, systems);
    }

    // Private

    private static class OEMEngine extends Engine {
	OEMEngine(IPlugin plugin, SystemEnumeration... systems) {
	    super(plugin, systems);
	}
    }

    private static void addExports(ComplexCheckType check, IScapContext ctx, Map<String, List<String>> qMap,
		Map<String, Variables> vMap) throws OcilException {

	for (Object obj : check.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		addExports((CheckType)obj, ctx, qMap, vMap);
	    } else if (obj instanceof ComplexCheckType) {
		addExports((ComplexCheckType)obj, ctx, qMap, vMap);
	    }
	}
    }

    private static void addExports(CheckType check, IScapContext ctx, Map<String, List<String>> qMap,
		Map<String, Variables> vMap) throws OcilException {

	if (check.getSystem().equals(SystemEnumeration.OCIL.namespace())) {
	    if (check.isSetCheckContentRef()) {
		Variables vars = null;
		for (CheckContentRefType ref : check.getCheckContentRef()) {
		    String href = ref.getHref();
		    String qId = ref.getName();
		    List<String> qIds = null;
		    if (qMap.containsKey(href)) {
			qIds = qMap.get(href);
		    } else {
			qIds = new ArrayList<String>();
			qMap.put(href, qIds);
		    }
		    if (qIds.contains(qId)) {
			qIds.add(qId);
		    }
		    if (vMap.containsKey(href)) {
			vars = vMap.get(href);
		    } else {
			vars = new Variables();
			vMap.put(href, vars);
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
	}
    }

    static class OcilExport implements IExport {
	private String href;
	private IChecklist checklist;
	private List<String> questionnaireIds;
	private IVariables variables;

	OcilExport(String href, IChecklist checklist, List<String> questionnaireIds, IVariables variables) {
	    this.href = href;
	    this.checklist = checklist;
	    this.questionnaireIds = questionnaireIds;
	    this.variables = variables;
	}

	// implement IExport

	public String getHref() {
	    return href;
	}

	public IChecklist getChecklist() {
	    return checklist;
	}

	public List<String> getQuestionnaireIds() {
	    return questionnaireIds;
	}

	public IVariables getVariables() {
	    return variables;
	}
    }
}
