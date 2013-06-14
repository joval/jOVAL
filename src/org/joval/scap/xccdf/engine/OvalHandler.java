// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import scap.oval.common.ClassEnumeration;
import scap.oval.common.GeneratorType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.DefinitionType;
import scap.oval.variables.VariableType;
import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckType;
import scap.xccdf.CheckExportType;
import scap.xccdf.InstanceResultType;
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
import org.joval.scap.ScapFactory;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.Producer;

/**
 * XCCDF helper class for OVAL processing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OvalHandler implements ISystem {
    private static final String NAMESPACE = SystemEnumeration.OVAL.namespace();

    private Map<String, EngineData> engines;
    private Map<String, IResults> results;
    private IScapContext ctx;
    private Producer<IXccdfEngine.Message> producer;

    public OvalHandler(IScapContext ctx, Producer<IXccdfEngine.Message> producer, Map<String, IResults> results) {
	this.ctx = ctx;
	this.producer = producer;
	this.results = results;
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
	    // TBD (DAS): inline content is not supported
	}

	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    String href = ref.getHref();
	    if (!results.containsKey(href)) {
		EngineData ed = null;
		if (engines.containsKey(href)) {
		    ed = engines.get(href);
		} else {
		    try {
			ed = new EngineData(ctx.getOval(href));
			engines.put(href, ed);
		    } catch (NoSuchElementException e) {
			continue;
		    }
		}

		//
		// Add definition references to the filter
		//
		if (ref.isSetName()) {
		    ed.getFilter().addDefinition(ref.getName());
		} else {
		    //
		    // Add all the definitions
		    //
		    IDefinitions definitions = ctx.getOval(href);
		    for (scap.oval.definitions.core.DefinitionType definition :
			 definitions.getOvalDefinitions().getDefinitions().getDefinition()) {
			ed.getFilter().addDefinition(definition.getId());
		    }
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

    public Map<String, ITransformable> exec(IPlugin plugin) {
	Map<String, ITransformable> reports = new HashMap<String, ITransformable>();
	reports.putAll(results);
	Iterator<Map.Entry<String, EngineData>> iter = engines.entrySet().iterator();
	while(iter.hasNext()) {
	    Map.Entry<String, EngineData> entry = iter.next();
	    String href = entry.getKey();
	    if (entry.getValue().createEngine(plugin)) {
		plugin.getLogger().info("Created engine for href " + href);
		IOvalEngine engine = entry.getValue().getEngine();
		producer.sendNotify(IXccdfEngine.Message.OVAL_ENGINE, engine);
		engine.run();
		switch(engine.getResult()) {
	  	  case OK:
		    reports.put(href, engine.getResults());
		    break;
		  case ERR:
		    engine.getError().printStackTrace();
		    break;
		}
	    } else {
		plugin.getLogger().info("No engine created for href " + href);
		iter.remove();
	    }
	}
	return reports;
    }

    public IResult getResult(CheckType check, boolean multi) throws OvalException, IllegalArgumentException {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}

	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    IResults ovalResult = null;
	    String href = ref.getHref();
	    if (results.containsKey(href)) {
		ovalResult = results.get(href);
	    } else if (engines.containsKey(href)) {
		IOvalEngine engine = engines.get(href).getEngine();
		switch(engine.getResult()) {
		  case OK:
		    ovalResult = engine.getResults();
		    break;
		  case ERR:
		    MessageType message = ScapFactory.XCCDF.createMessageType();
		    message.setSeverity(MsgSevEnumType.ERROR);
		    message.setValue(engine.getError().getMessage());
		    CheckResult cr = new CheckResult(ResultEnumType.ERROR, check);
		    cr.addMessage(message);
		    return cr;
		}
	    }
	    if (ovalResult != null) {
		CheckData data = new CheckData(check.getNegate());
		if (ref.isSetName()) {
		    try {
			String definitionId = ref.getName();
			ClassEnumeration definitionClass = ovalResult.getDefinition(definitionId).getClazz();
			ResultEnumeration definitionResult = ovalResult.getDefinitionResult(definitionId);
			data.add(convertResult(definitionClass, definitionResult));
		    } catch (NoSuchElementException e) {
			data.add(ResultEnumType.UNKNOWN);
		    }
		} else if (multi) {
		    CheckResult cr = new CheckResult();
		    for (DefinitionType def : ovalResult.getDefinitionResults()) {
			data = new CheckData(check.getNegate());
			String definitionId = def.getDefinitionId();
			ClassEnumeration definitionClass = ovalResult.getDefinition(definitionId).getClazz();
			ResultEnumeration definitionResult = ovalResult.getDefinitionResult(definitionId);
			data.add(convertResult(definitionClass, definitionResult));
			InstanceResultType inst = Engine.FACTORY.createInstanceResultType();
			inst.setValue(def.getDefinitionId());
			cr.getResults().add(new CheckResult(data.getResult(CcOperatorEnumType.AND), check, inst));
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
		return new CheckResult(data.getResult(CcOperatorEnumType.AND), check);
	    }
	}
	return new CheckResult(ResultEnumType.NOTCHECKED, check);
    }

    // Private

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

	EngineData(IDefinitions definitions) {
	    this.definitions = definitions;
	    filter = OvalFactory.createDefinitionFilter();
	    variables = OvalFactory.createVariables();
	}

	IDefinitionFilter getFilter() {
	    return filter;
	}

	IVariables getVariables() {
	    return variables;
	}

	boolean createEngine(IPlugin plugin) {
	    if (filter.size() > 0) {
		engine = OvalFactory.createEngine(IOvalEngine.Mode.DIRECTED, plugin);
		engine.setDefinitions(definitions);
		engine.setExternalVariables(variables);
		engine.setDefinitionFilter(filter);
		return true;
	    } else {
		return false;
	    }
	}

	IOvalEngine getEngine() {
	    return engine;
	}
    }
}
