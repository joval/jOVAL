// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.oval.common.ClassEnumeration;
import scap.oval.common.GeneratorType;
import scap.oval.definitions.core.CriteriaType;
import scap.oval.definitions.core.CriterionType;
import scap.oval.definitions.core.ExtendDefinitionType;
import scap.oval.definitions.core.ObjectRefType;
import scap.oval.definitions.core.TestType;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.DefinitionType;
import scap.oval.systemcharacteristics.core.ObjectType;
import scap.oval.systemcharacteristics.core.VariableValueType;
import scap.oval.variables.VariableType;
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckExportType;
import scap.xccdf.CheckImportType;
import scap.xccdf.CheckType;
import scap.xccdf.MsgSevEnumType;
import scap.xccdf.MessageType;
import scap.xccdf.ResultEnumType;

import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.IVariables;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.scap.ScapFactory;
import org.joval.scap.oval.Definitions;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.Producer;

/**
 * XCCDF helper class for OVAL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OvalHandler implements ISystem {
    private static final String EMPTY_URI = "http://www.gocil.org/checklists/empty.xml";
    private static final IDefinitions EMPTY_DEFINITIONS = new Definitions();

    private static final String NAMESPACE = SystemEnumeration.OVAL.namespace();

    private Map<String, EngineData> engines;
    private Map<String, IResults> results;
    private IScapContext ctx;
    private Producer<IXccdfEngine.Message> producer;
    private boolean cancelled = false;

    public OvalHandler(IScapContext ctx, Producer<IXccdfEngine.Message> producer, Map<String, IResults> results) {
	this.ctx = ctx;
	this.producer = producer;
	if (results == null) {
	    this.results = new HashMap<String, IResults>();
	} else {
	    this.results = results;
	}
	engines = new HashMap<String, EngineData>();
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
	    String href = ref.getHref();
	    if (!results.containsKey(href)) {
		EngineData ed = null;
		if (engines.containsKey(href)) {
		    ed = engines.get(href);
		} else {
		    ed = new EngineData(href, ctx.getOval(href));
		    engines.put(href, ed);
		}

		//
		// Add definition references to the filter
		//
		if (ref.isSetName()) {
		    ed.getFilter().addDefinition(ref.getName());
		}

		//
		// Add variable exports to the variables
		//
		for (CheckExportType export : check.getCheckExport()) {
		    String ovalVariableId = export.getExportName();
		    String valueId = export.getValueId();
		    for (String s : ctx.getValues().get(valueId)) {
			ed.getVariables().addValue(ovalVariableId, s);
		    }
		    ed.getVariables().setComment(ovalVariableId, valueId);
		}
	    }
	}
    }

    public Map<String, ? extends ITransformable<?>> exec(IPlugin plugin) {
	if (engines.size() == 0) {
	    engines.put(EMPTY_URI, new EngineData(EMPTY_URI, EMPTY_DEFINITIONS));
	}
	Iterator<Map.Entry<String, EngineData>> iter = engines.entrySet().iterator();
	while(iter.hasNext()) {
	    if (cancelled) {
		break;
	    }
	    Map.Entry<String, EngineData> entry = iter.next();
	    String href = entry.getKey();
	    if (entry.getValue().createEngine(plugin)) {
		plugin.getLogger().info("Created engine for href " + href);
		IOvalEngine engine = entry.getValue().getEngine();
		producer.sendNotify(IXccdfEngine.Message.OVAL_ENGINE, engine);
		engine.run();
		switch(engine.getResult()) {
	  	  case OK:
		    results.put(href, engine.getResults());
		    break;
		  case ERR:
		    plugin.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), engine.getError());
		    break;
		}
	    } else {
		plugin.getLogger().warn("No engine created for href " + href);
		iter.remove();
	    }
	}
	return results;
    }

    public void cancelExec(boolean hard) {
	cancelled = true;
	Iterator<EngineData> iter = engines.values().iterator();
	while(iter.hasNext()) {
	    EngineData ed = iter.next();
	    iter.remove();
	    IOvalEngine engine = ed.getEngine();
	    if (engine != null) {
		engine.cancelScan(hard);
	    }
	}
    }

    public IResult getResult(CheckType check) throws OvalException, IllegalArgumentException {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}
	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    String href = ref.getHref();
	    if (results.containsKey(href)) {
		return getResult(check, ref, results.get(href));
	    } else if (engines.containsKey(href)) {
		IOvalEngine engine = engines.get(href).getEngine();
		switch(engine.getResult()) {
		  case OK:
		    return getResult(check, ref, engine.getResults());
		  case ERR:
		    MessageType message = ScapFactory.XCCDF.createMessageType();
		    message.setSeverity(MsgSevEnumType.ERROR);
		    message.setValue(engine.getError().getMessage());
		    CheckResult cr = new CheckResult(ResultEnumType.ERROR, check);
		    cr.addMessage(message);
		    return cr;
		}
	    }
	}
	return new CheckResult(ResultEnumType.NOTCHECKED, check);
    }

    // Private

    private IResult getResult(CheckType check, CheckContentRefType ref, IResults ovalResult) throws OvalException {
	CheckType checkResult = Engine.FACTORY.createCheckType();
	checkResult.setId(check.getId());
	checkResult.setMultiCheck(check.getMultiCheck());
	checkResult.setNegate(check.getNegate());
	checkResult.setSelector(check.getSelector());
	checkResult.setSystem(NAMESPACE);
	checkResult.getCheckExport().addAll(check.getCheckExport());
	checkResult.getCheckContentRef().add(ref);
	if (check.isSetCheckImport()) {
	    Collection<String> objectIds = new HashSet<String>();
	    getObjectReferences(ref.getName(), ovalResult.getDefinitions(), new HashSet<String>(), objectIds);
	    for (String objectId : objectIds) {
		ObjectType object = ovalResult.getSystemCharacteristics().getObject(objectId);
		if (object.isSetVariableValue()) {
		    for (CheckImportType cit : check.getCheckImport()) {
			String variableId = cit.getImportName();
			List<Object> values = new ArrayList<Object>();
			for (VariableValueType var : object.getVariableValue()) {
			    if (variableId.equals(var.getVariableId()) && var.isSetValue()) {
				values.add(var.getValue());
			    }
			}
			CheckImportType copy = Engine.FACTORY.createCheckImportType();
			copy.setImportName(variableId);
			if (values.size() > 0) {
			    copy.getContent().addAll(values);
			}
			checkResult.getCheckImport().add(copy);
		    }
		}
	    }
	}

	CheckData data = new CheckData(check.getNegate());
	if (ref.isSetName()) {
	    String definitionId = ref.getName();
	    try {
		ClassEnumeration definitionClass = ovalResult.getDefinition(definitionId).getClazz();
		ResultEnumeration definitionResult = ovalResult.getDefinitionResult(definitionId);
		data.add(convertResult(definitionClass, definitionResult));
	    } catch (NoSuchElementException e) {
		CheckResult cr = new CheckResult(ResultEnumType.ERROR, check);
		MessageType message = ScapFactory.XCCDF.createMessageType();
		message.setSeverity(MsgSevEnumType.ERROR);
		message.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_DEFINITION, definitionId));
		cr.addMessage(message);
		return cr;
	    }
	} else if (check.getMultiCheck()) {
	    CheckResult cr = new CheckResult();
	    for (DefinitionType def : ovalResult.getDefinitionResults()) {
		data = new CheckData(check.getNegate());
		String definitionId = def.getDefinitionId();
		ClassEnumeration definitionClass = ovalResult.getDefinition(definitionId).getClazz();
		ResultEnumeration definitionResult = ovalResult.getDefinitionResult(definitionId);
		data.add(convertResult(definitionClass, definitionResult));
		CheckType ct = Engine.copy(checkResult);
		ct.getCheckContentRef().get(0).setName(def.getDefinitionId());
		cr.getResults().add(new CheckResult(data.getResult(CcOperatorEnumType.AND), ct));
	    }
	    return cr;
	} else {
	    for (DefinitionType def : ovalResult.getDefinitionResults()) {
		String definitionId = def.getDefinitionId();
		ClassEnumeration definitionClass = def.getClazz();
		ResultEnumeration definitionResult = ovalResult.getDefinitionResult(definitionId);
		data.add(convertResult(definitionClass, definitionResult));
	    }
	}
	return new CheckResult(data.getResult(CcOperatorEnumType.AND), checkResult);
    }

    private void getObjectReferences(Object obj, IDefinitions defs, Collection<String> visited, Collection<String> result) {
	if (obj instanceof String) {
	    String defId = (String)obj;
	    try {
		getObjectReferences(defs.getDefinition(defId).getCriteria(), defs, visited, result);
	    } catch (NoSuchElementException e) {
		// definition was not found; skip it
	    }
	} else if (obj instanceof CriteriaType) {
	    for (Object child : ((CriteriaType)obj).getCriteriaOrCriterionOrExtendDefinition()) {
		getObjectReferences(child, defs, visited, result);
	    }
	} else if (obj instanceof CriterionType) {
	    try {
		TestType tt = defs.getTest(((CriterionType)obj).getTestRef()).getValue();
		Method m = tt.getClass().getMethod("getObject");
		String objectId = ((ObjectRefType)m.invoke(tt)).getObjectRef();
		result.add(objectId);
	    } catch (Exception e) {
	    }
	} else if (obj instanceof ExtendDefinitionType) {
	    ExtendDefinitionType edt = (ExtendDefinitionType)obj;
	    if (!visited.contains(edt.getDefinitionRef())) {
		getObjectReferences(edt.getDefinitionRef(), defs, visited, result);
	    }
	}
    }

    /**
     * Map an OVAL result to an XCCDF result.
     *
     * @see the SCAP specification document, Section 4.5.2: Mapping OVAL Results to XCCDF Results
     *
     */
    private ResultEnumType convertResult(ClassEnumeration ce, ResultEnumeration re) {
	switch (re) {
	  case ERROR:
	    return ResultEnumType.ERROR;
    
	  case FALSE:
	    switch(ce) {
	      case VULNERABILITY:
	      case PATCH:
		return ResultEnumType.PASS;

	      case COMPLIANCE:
	      case INVENTORY:
	      case MISCELLANEOUS:
	      default:
		return ResultEnumType.FAIL;
	    }

	  case TRUE:
	    switch(ce) {
	      case VULNERABILITY:
	      case PATCH:
		return ResultEnumType.FAIL;

	      case COMPLIANCE:
	      case INVENTORY:
	      case MISCELLANEOUS:
	      default:
		return ResultEnumType.PASS;
	    }
 
	  case NOT_APPLICABLE:
	    return ResultEnumType.NOTAPPLICABLE;
 
	  case NOT_EVALUATED:
	    return ResultEnumType.NOTCHECKED;
 
	  case UNKNOWN:
	  default:
	    return ResultEnumType.UNKNOWN;
	}
    }

    class EngineData {
	private IDefinitions definitions;
	private IDefinitionFilter filter;
	private IVariables variables;
	private IOvalEngine engine;
	private String uri;

	EngineData(String uri, IDefinitions definitions) {
	    this.uri = uri;
	    this.definitions = definitions;
	    variables = OvalFactory.createVariables();
	}

	IDefinitionFilter getFilter() {
	    if (filter == null) {
		filter = OvalFactory.createDefinitionFilter();
	    }
	    return filter;
	}

	IVariables getVariables() {
	    return variables;
	}

	boolean createEngine(IPlugin plugin) {
	    if (plugin == null) {
		return false;
	    } else {
		engine = OvalFactory.createEngine(IOvalEngine.Mode.DIRECTED, plugin);
		engine.setDefinitions(definitions);
		engine.setExternalVariables(variables);
		if (filter != null) {
		    engine.setDefinitionFilter(filter);
		}
		return true;
	    }
	}

	IOvalEngine getEngine() {
	    return engine;
	}
    }
}
