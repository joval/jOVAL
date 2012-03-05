// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Hashtable;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.plugin.IRequestContext;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;

class RequestContext implements IRequestContext {
    private Engine engine;
    private ObjectType object;
    private Hashtable<String, VariableValueType> vars;

    RequestContext(Engine engine, ObjectType object) {
	this.engine = engine;
        this.object = object;
        this.vars = new Hashtable<String, VariableValueType>();
    }

    Collection<VariableValueType> getVars() {
        return vars.values();
    }

    void addVar(VariableValueType var) {
	vars.put(var.getVariableId(), var);
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
