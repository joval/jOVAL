// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import org.w3c.dom.Document;

import jsaf.intf.system.IComputerSystem;
import jsaf.intf.system.ISession;
import jsaf.util.StringTools;

import org.openscap.sce.xccdf.ScriptDataType;
import org.openscap.sce.results.SceResultsType;
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.MsgSevEnumType;
import scap.xccdf.MessageType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.scap.ScapFactory;
import org.joval.scap.sce.Result;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;
import org.joval.xml.XPathTools;

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
    private boolean cancelled = false;

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
	if (check.isSetCheckContent()) {
	    throw new ScapException(JOVALMsg.getMessage(JOVALMsg.ERROR_SCAP_CHECKCONTENT));
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

    public Map<String, ? extends ITransformable<?>> exec(IPlugin plugin) {
	if (plugin.getSession() instanceof IComputerSystem) {
	    IComputerSystem session = (IComputerSystem)plugin.getSession();
	    for (Map.Entry<String, Wrapper> entry : scripts.entrySet()) {
		if (cancelled) {
		    break;
		}
		String href = entry.getKey();
		Wrapper wrapper = entry.getValue();
		producer.sendNotify(IXccdfEngine.Message.SCE_SCRIPT, href);
		IScriptResult result = null;
		try {
		    result = wrapper.getScript().exec(wrapper.getExports(), session);
		} catch (Exception e) {
		    result = new Result(href, e);
		}
		results.put(href, result);
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORM, plugin.getSession().getType());
	    Exception error = new Exception(msg);
	    for (Map.Entry<String, Wrapper> entry : scripts.entrySet()) {
		String href = entry.getKey();
		results.put(href, new Result(href, error));
	    }
	}
	return results;
    }

    public void destroy() {
	cancelled = true;
    }

    public IResult getResult(CheckType check) throws IllegalArgumentException {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
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
		    checkResult.getCheckContentRef().add(ref);
		    IScriptResult sr = results.get(ref.getHref());
		    SceResultsType srt = sr.getRootObject().getValue();

		    List<Exception> errors = new ArrayList<Exception>();
		    for (CheckImportType cit : check.getCheckImport()) {
			if ("stdout".equals(cit.getImportName()) && srt.isSetStdout()) {
			    CheckImportType copy = Engine.FACTORY.createCheckImportType();
			    copy.setImportName("stdout");
			    if (cit.isSetImportXpath()) {
				try {
				    String xpath = cit.getImportXpath();
				    byte[] data = srt.getStdout().getBytes(StringTools.UTF8);
				    Document doc = XPathTools.parse(new ByteArrayInputStream(data));
				    XPathExpression expr = XPathTools.compile(xpath);
				    copy.getContent().addAll(XPathTools.typesafeEval(expr, doc));
				} catch (Exception e) {
				    errors.add(e);
				}
			    } else {
				copy.getContent().add(srt.getStdout());
			    }
			    checkResult.getCheckImport().add(copy);
			}
		    }

		    CheckData data = new CheckData(check.getNegate());
		    data.add(srt.getResult());
		    CheckResult cr = new CheckResult(data.getResult(CcOperatorEnumType.AND), checkResult);
		    if (sr.hasError()) {
			MessageType message = ScapFactory.XCCDF.createMessageType();
			message.setSeverity(MsgSevEnumType.ERROR);
			message.setValue(sr.getError().getMessage());
			cr.addMessage(message);
		    }
		    for (Exception err : errors) {
			MessageType message = ScapFactory.XCCDF.createMessageType();
			message.setSeverity(MsgSevEnumType.ERROR);
			message.setValue(err.getMessage());
			cr.addMessage(message);
		    }
		    return cr;
		}
	    }
	}
	return new CheckResult(ResultEnumType.NOTCHECKED, check);
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
