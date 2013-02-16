// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jsaf.intf.system.ISession;

import org.openscap.sce.xccdf.ScriptDataType;
import org.openscap.sce.results.SceResultsType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.ObjectFactory;
import scap.xccdf.OverrideableCPE2IdrefType;
import scap.xccdf.ResultEnumType;
import scap.xccdf.RoleEnumType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;
import scap.xccdf.TestResultType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.xccdf.IEngine;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.sce.SceException;
import org.joval.scap.sce.SCEScript;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.RuleResult;
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

    private IView view;
    private Producer<IEngine.Message> producer;
    private Map<String, Script> scripts;
    private Map<String, SceResultsType> results;

    /**
     * Create an OVAL handler utility for the given XCCDF and Profile.
     */
    public SceHandler(IView view, Producer<IEngine.Message> producer) {
	this.view = view;
	this.producer = producer;
	scripts = new HashMap<String, Script>();
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
		String scriptId = ref.getHref();
		Map<String, String> exports = new HashMap<String, String>();
		for (CheckExportType export : check.getCheckExport()) {
		    exports.put(export.getExportName(), getSingleValue(export.getValueId()));
		}
		scripts.put(scriptId, new Script(scriptId, exports));
	    }
	}
    }

    public Collection<ITransformable> exec(IPlugin plugin) throws Exception {
	Collection<ITransformable> reports = new ArrayList<ITransformable>();
	results = new HashMap<String, SceResultsType>();
	ISession session = plugin.getSession();
	for (Map.Entry<String, Script> entry : scripts.entrySet()) {
	    Script rs = entry.getValue();
	    producer.sendNotify(IEngine.Message.SCE_SCRIPT, entry.getKey());
//DAS: TBD create an ITransformable to contain the SceResultsType...
	    SceResultsType srt = new SCEScript(rs.getExports(), rs.getData(), session).exec();
	    results.put(entry.getKey(), srt);
	}
	return reports;
    }

    public Object getResult(CheckType check) throws Exception {
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

	RuleResult result = new RuleResult(check.getNegate());
	if (check.isSetCheckContentRef()) {
	    for (CheckContentRefType ref : check.getCheckContentRef()) {
		if (results.containsKey(ref.getHref())) {
		    SceResultsType srt = results.get(ref.getHref());
		    result.add(srt.getResult());

		    // DAS: need a mechanism to return the CheckType back to the engine...
		    CheckType checkResult = Engine.FACTORY.createCheckType();
		    checkResult.getCheckContentRef().add(ref);
		    checkResult.getCheckExport().addAll(check.getCheckExport());
		    if (importStdout) {
			CheckImportType cit = Engine.FACTORY.createCheckImportType();
			cit.setImportName("stdout");
			cit.getContent().add(srt.getStdout());
			checkResult.getCheckImport().add(cit);
		    }
		}
	    }
	}
	return result.getResult();
    }

    // Private

    private String getSingleValue(String id) throws SceException {
	Collection<String> values = view.getValues().get(id);
	if (values.size() == 1) {
	    return values.iterator().next();
	} else {
	    throw new SceException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_VARS, id, values.size()));
	}
    }

    class Script {
	String id;
	Map<String, String> exports;
	ScriptDataType data;

	Script(String id, Map<String, String> exports) throws NoSuchElementException, SceException {
	    this.id = id;
	    data = view.getStream().getSce(id);
	    this.exports = exports;
	}

	Map<String, String> getExports() {
	    return exports;
	}

	ScriptDataType getData() {
	    return data;
	}
    }
}
