// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.plugin.IRequestContext;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALSystem;

class RequestContext implements IRequestContext {
    private Engine engine;
    private ObjectType object;
    private Hashtable<String, HashSet<String>> vars;

    RequestContext(Engine engine, ObjectType object) {
	this.engine = engine;
        this.object = object;
        this.vars = new Hashtable<String, HashSet<String>>();
    }

    Collection<VariableValueType> getVars() {
	Collection<VariableValueType> result = new Vector<VariableValueType>();
	for (String id : vars.keySet()) {
	    for (String value : vars.get(id)) {
		VariableValueType variableValueType = JOVALSystem.factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(id);
		variableValueType.setValue(value);
		result.add(variableValueType);
	    }
	}
        return result;
    }

    void addVar(VariableValueType var) {
	String id = var.getVariableId();
	String value = (String)var.getValue();
	if (vars.containsKey(id)) {
	    vars.get(id).add(value);
	} else {
	    HashSet<String> vals = new HashSet<String>();
	    vals.add(value);
	    vars.put(id, vals);
	}
    }

    // Implement IRequestContext

    public ObjectType getObject() {
        return object;
    }

    public void addMessage(MessageType msg) {
        engine.getSystemCharacteristics().setObject(object.getId(), null, null, null, msg);
    }

    public Collection<String> resolve(String variableId) throws NoSuchElementException, ResolveException, OvalException {
        return engine.resolve(variableId, this);
    }
}
