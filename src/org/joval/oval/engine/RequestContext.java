// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.plugin.IRequestContext;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

class RequestContext implements IRequestContext {
    private Engine engine;
    private Stack<ObjectType> objects;
    private Hashtable<String, HashSet<String>> vars;
    private Collection<MessageType> messages;

    RequestContext(Engine engine, ObjectType object) {
	this.engine = engine;
        this.objects = new Stack<ObjectType>();
	objects.push(object);
        this.vars = new Hashtable<String, HashSet<String>>();
	this.messages = new Vector<MessageType>();
    }

    Collection<VariableValueType> getVars() {
	Collection<VariableValueType> result = new Vector<VariableValueType>();
	for (String id : vars.keySet()) {
	    for (String value : vars.get(id)) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(id);
		variableValueType.setValue(value);
		result.add(variableValueType);
	    }
	}
        return result;
    }

    Collection<MessageType> getMessages() {
	return messages;
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

    ObjectType getObject() {
        return objects.peek();
    }

    void pushObject(ObjectType obj) {
	objects.push(obj);
    }

    ObjectType popObject() {
	return objects.pop();
    }

    // Implement IRequestContext

    public void addMessage(MessageType msg) {
	messages.add(msg);
    }
}
