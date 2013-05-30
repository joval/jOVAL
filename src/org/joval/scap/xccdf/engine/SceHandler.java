// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jsaf.intf.system.ISession;

import org.openscap.sce.xccdf.ScriptDataType;
import org.openscap.sce.results.SceResultsType;
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;

/**
 * XCCDF helper class for SCE processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SceHandler implements ISystem {
    public static final String NAMESPACE = SystemEnumeration.SCE.namespace();

    private IScapContext ctx;
    private Producer<IXccdfEngine.Message> producer;
    private Map<String, Wrapper> scripts;
    private Map<String, IScriptResult> results;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SceHandler(IScapContext ctx, Producer<IXccdfEngine.Message> producer, Map<String, IScriptResult> results) {
	this.ctx = ctx;
	this.producer = producer;
	this.results = results;
	scripts = new HashMap<String, Wrapper>();
    }

    // Implement ISystem

    public String getNamespace() {
	return NAMESPACE;
    }

    public void add(CheckType check) throws Exception {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    if (ref.isSetHref()) {
		String href = ref.getHref();
		if (!results.containsKey(href)) {
		    Map<String, String> exports = new HashMap<String, String>();
		    for (CheckExportType export : check.getCheckExport()) {
			exports.put(export.getExportName(), getSingleValue(export.getValueId()));
		    }
		    scripts.put(href, new Wrapper(href, exports));
		}
	    }
	}
    }

    public Map<String, ITransformable> exec(IPlugin plugin) throws Exception {
	Map<String, ITransformable> reports = new HashMap<String, ITransformable>();
	reports.putAll(results);
	ISession session = plugin.getSession();
	for (Map.Entry<String, Wrapper> entry : scripts.entrySet()) {
	    String href = entry.getKey();
	    Wrapper wrapper = entry.getValue();
	    producer.sendNotify(IXccdfEngine.Message.SCE_SCRIPT, href);
	    IScriptResult result = wrapper.getScript().exec(wrapper.getExports(), session);
	    reports.put(href, result);
	}
	return reports;
    }

    public IResult getResult(CheckType check, boolean multi) throws Exception {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}

	boolean importStdout = false;
	for (CheckImportType cit : check.getCheckImport()) {
	    if ("stdout".equals(cit.getImportName())) {
		importStdout = true;
		break;
	    }
	}

	CheckData data = new CheckData(check.getNegate());
	CheckType checkResult = Engine.FACTORY.createCheckType();
	checkResult.setId(check.getId());
	checkResult.setMultiCheck(false);
	checkResult.setNegate(check.getNegate());
	checkResult.setSelector(check.getSelector());
	checkResult.setSystem(NAMESPACE);
	checkResult.getCheckExport().addAll(check.getCheckExport());

	if (check.isSetCheckContentRef()) {
	    for (CheckContentRefType ref : check.getCheckContentRef()) {
		if (results.containsKey(ref.getHref())) {
		    SceResultsType srt = results.get(ref.getHref()).getResult();
		    data.add(srt.getResult());
		    checkResult.getCheckContentRef().add(ref);
		    if (importStdout) {
			CheckImportType cit = Engine.FACTORY.createCheckImportType();
			cit.setImportName("stdout");
			cit.getContent().add(srt.getStdout());
			checkResult.getCheckImport().add(cit);
		    }
		    break;
		}
	    }
	}
	return new CheckResult(data.getResult(CcOperatorEnumType.AND), checkResult);
    }

    // Private

    private String getSingleValue(String id) throws SceException {
	Collection<String> values = ctx.getValues().get(id);
	if (values.size() == 1) {
	    return values.iterator().next();
	} else {
	    throw new SceException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_VARS, id, values.size()));
	}
    }

    class Wrapper {
	String id;
	Map<String, String> exports;
	IScript script;

	Wrapper(String id, Map<String, String> exports) throws NoSuchElementException, SceException {
	    this.id = id;
	    script = ctx.getSce(id);
	    this.exports = exports;
	}

	Map<String, String> getExports() {
	    return exports;
	}

	IScript getScript() {
	    return script;
	}
    }
}
