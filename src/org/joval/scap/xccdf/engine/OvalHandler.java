// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

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
import scap.xccdf.ObjectFactory;
import scap.xccdf.ResultEnumType;

import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.IVariables;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.IEngine.Message;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.xml.ITransformable;
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
    private IView view;
    private Producer<Message> producer;

    public OvalHandler(IView view, Producer<Message> producer) {
	this.view = view;
	this.producer = producer;
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
	    EngineData ed = null;
	    if (engines.containsKey(href)) {
		ed = engines.get(href);
	    } else {
		try {
		    ed = new EngineData(view.getStream().getOval(href));
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
		IDefinitions definitions = view.getStream().getOval(href);
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
		for (String s : view.getValues().get(valueId)) {
		    ed.getVariables().addValue(ovalVariableId, s);
		}
		ed.getVariables().setComment(ovalVariableId, valueId);
	    }
	}
    }

    public Collection<ITransformable> exec(IPlugin plugin) throws Exception {
	Collection<ITransformable> reports = new ArrayList<ITransformable>();
	Iterator<Map.Entry<String, EngineData>> iter = engines.entrySet().iterator();
	while(iter.hasNext()) {
	    Map.Entry<String, EngineData> entry = iter.next();
	    if (entry.getValue().createEngine(plugin)) {
		plugin.getLogger().info("Created engine for href " + entry.getKey());
		IEngine engine = entry.getValue().getEngine();
		producer.sendNotify(Message.OVAL_ENGINE, engine);
		engine.run();
		switch(engine.getResult()) {
	  	  case OK:
		    reports.add(engine.getResults());
		    break;
	  	  case ERR:
		    throw engine.getError();
		}
	    } else {
		plugin.getLogger().info("No engine created for href " + entry.getKey());
		iter.remove();
	    }
	}
	return reports;
    }

    public IResult getResult(CheckType check, boolean multi) throws Exception {
	if (!NAMESPACE.equals(check.getSystem())) {
	    throw new IllegalArgumentException(check.getSystem());
	}

	for (CheckContentRefType ref : check.getCheckContentRef()) {
	    if (engines.containsKey(ref.getHref())) {
		CheckData data = new CheckData(check.getNegate());
		IResults ovalResult = engines.get(ref.getHref()).getEngine().getResults();
		if (ref.isSetName()) {
		    try {
			data.add(convertResult(ovalResult.getDefinitionResult(ref.getName())));
		    } catch (NoSuchElementException e) {
			data.add(ResultEnumType.UNKNOWN);
		    }
		} else if (multi) {
		    CheckResult cr = new CheckResult();
		    for (DefinitionType def : ovalResult.getDefinitionResults()) {
			data = new CheckData(check.getNegate());
			data.add(convertResult(def.getResult()));
			InstanceResultType inst = Engine.FACTORY.createInstanceResultType();
			inst.setValue(def.getDefinitionId());
			cr.getResults().add(new CheckResult(data.getResult(CcOperatorEnumType.AND), check, inst));
		    }
		    return cr;
		} else {
		    for (DefinitionType def : ovalResult.getDefinitionResults()) {
			data.add(convertResult(def.getResult()));
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
     */
    private ResultEnumType convertResult(ResultEnumeration re) {
	switch (re) {
	  case ERROR:
	    return ResultEnumType.ERROR;
    
	  case FALSE:
	    return ResultEnumType.FAIL;
  
	  case TRUE:
	    return ResultEnumType.PASS;
 
	  case UNKNOWN:
	  default:
	    return ResultEnumType.UNKNOWN;
	}
    }

    class EngineData {
	private IDefinitions definitions;
	private IDefinitionFilter filter;
	private IVariables variables;
	private IEngine engine;

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
		engine = OvalFactory.createEngine(IEngine.Mode.DIRECTED, plugin);
		engine.setDefinitions(definitions);
		engine.setExternalVariables(variables);
		engine.setDefinitionFilter(filter);
		return true;
	    } else {
		return false;
	    }
	}

	IEngine getEngine() {
	    return engine;
	}
    }
}
