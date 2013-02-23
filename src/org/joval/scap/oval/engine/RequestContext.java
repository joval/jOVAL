// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.VariableValueType;

import org.joval.intf.scap.oval.IProvider;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.OvalException;
import org.joval.util.JOVALMsg;

class RequestContext implements IProvider.IRequestContext {
    private Stack<Level> levels;

    RequestContext(ObjectType object) {
        levels = new Stack<Level>();
	levels.push(new Level(object));
    }

    Collection<VariableValueType> getVars() {
	return getVars(levels.peek().vars);
    }

    Collection<MessageType> getMessages() {
	return levels.peek().messages;
    }

    void addVar(VariableValueType var) {
	String id = var.getVariableId();
	String value = (String)var.getValue();
	Hashtable<String, HashSet<String>> vars = levels.peek().vars;
	if (vars.containsKey(id)) {
	    vars.get(id).add(value);
	} else {
	    HashSet<String> vals = new HashSet<String>();
	    vals.add(value);
	    vars.put(id, vals);
	}
    }

    ObjectType getObject() {
        return levels.peek().object;
    }

    void pushObject(ObjectType obj) {
	levels.push(new Level(obj));
    }

    ObjectType popObject() {
	Level level = levels.pop();
	for (VariableValueType var : getVars(level.vars)) {
	    addVar(var);
	}
	levels.peek().messages.addAll(level.messages);
	return level.object;
    }

    // Implement IRequestContext

    public void addMessage(MessageType msg) {
	levels.peek().messages.add(msg);
    }

    // Private

    private Collection<VariableValueType> getVars(Hashtable<String, HashSet<String>> vars) {
	Collection<VariableValueType> result = new ArrayList<VariableValueType>();
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

    private class Level {
	ObjectType object;
	Hashtable<String, HashSet<String>> vars;
	Collection<MessageType> messages;

	Level(ObjectType object) {
	    this.object = object;
            this.vars = new Hashtable<String, HashSet<String>>();
	    this.messages = new ArrayList<MessageType>();
	}
    }
}
