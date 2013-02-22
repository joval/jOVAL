// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.oval.definitions.core.DefinitionType;
import scap.oval.definitions.core.DefinitionsType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.ObjectsType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.definitions.core.StateType;
import scap.oval.definitions.core.StatesType;
import scap.oval.definitions.core.TestType;
import scap.oval.definitions.core.TestsType;
import scap.oval.definitions.core.VariableType;
import scap.oval.definitions.core.VariablesType;

import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Index to an OvalDefinitions object, for fast look-up of definitions, tests, variables, objects and states.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Definitions implements IDefinitions, ILoggable {
    public static final OvalDefinitions getOvalDefinitions(File f) throws OvalException {
	return getOvalDefinitions(new StreamSource(f));
    }

    public static final OvalDefinitions getOvalDefinitions(InputStream in) throws OvalException {
	return getOvalDefinitions(new StreamSource(in));
    }

    public static final OvalDefinitions getOvalDefinitions(Source source) throws OvalException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OvalDefinitions) {
		return (OvalDefinitions)rootObj;
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITIONS_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private OvalDefinitions defs;
    private LocLogger logger;
    private Hashtable<String, DefinitionType> definitions;
    private Hashtable<String, TestType> tests;
    private Hashtable<String, StateType> states;
    private Hashtable<String, VariableType> variables;
    private Hashtable<String, ObjectType> objects;

    /**
     * Load OVAL definitions from a File.
     */
    Definitions(File f) throws OvalException {
	this(getOvalDefinitions(f));
    }

    Definitions(InputStream in) throws OvalException {
	this(getOvalDefinitions(in));
    }

    public Definitions(OvalDefinitions defs) throws OvalException {
	this.defs = defs;
	this.logger = JOVALMsg.getLogger();

	objects = new Hashtable <String, ObjectType>();
	if (defs.getObjects() != null) {
	    for (JAXBElement<? extends ObjectType> jot : defs.getObjects().getObject()) {
		ObjectType ot = jot.getValue();
		objects.put(ot.getId(), ot);
	    }
	}

	tests = new Hashtable <String, TestType>();
	if (defs.isSetTests() && defs.getTests().isSetTest()) {
	    for (JAXBElement<? extends TestType> jtt : defs.getTests().getTest()) {
		TestType tt = jtt.getValue();
		tests.put(tt.getId(), tt);
	    }
	}

	variables = new Hashtable <String, VariableType>();
	if (defs.getVariables() != null) {
	    for (JAXBElement<? extends VariableType> jvt : defs.getVariables().getVariable()) {
		VariableType vt = jvt.getValue();
		variables.put(vt.getId(), vt);
	    }
	}

	states = new Hashtable <String, StateType>();
	if (defs.getStates() != null) {
	    for (JAXBElement<? extends StateType> jst : defs.getStates().getState()) {
		StateType st = jst.getValue();
		states.put(st.getId(), st);
	    }
	}

	definitions = new Hashtable <String, DefinitionType>();
	if (defs.isSetDefinitions() && defs.getDefinitions().isSetDefinition()) {
	    for (DefinitionType dt : defs.getDefinitions().getDefinition()) {
		definitions.put(dt.getId(), dt);
	    }
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext(), getOvalDefinitions());
    }

    public Object getRootObject() {
	return getOvalDefinitions(); 
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext();
    }

    // Implement IDefinitions

    public OvalDefinitions getOvalDefinitions() {
	return defs;
    }

    public <T extends ObjectType> T getObject(String id, Class<T> type) throws NoSuchElementException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_DEFINITION, id));
	} else if (type.isInstance(object)) {
	    return type.cast(object);
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_INSTANCE, type.getName(), object.getClass().getName());
	    throw new NoSuchElementException(msg);
	}
    }

    public Collection<ObjectType> getObjects() {
	return objects.values();
    }

    public StateType getState(String id) throws NoSuchElementException {
	StateType state = states.get(id);
	if (state == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_STATE, id));
	}
	return state;
    }

    public TestType getTest(String id) throws NoSuchElementException {
	TestType test = tests.get(id);
	if (test == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_TEST, id));
	}
	return test;
    }

    public ObjectType getObject(String id) throws NoSuchElementException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	return object;
    }

    public VariableType getVariable(String id) throws NoSuchElementException {
	VariableType variable = variables.get(id);
	if (variable == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_VARIABLE, id));
	} else {
	    return variable;
	}
    }

    public DefinitionType getDefinition(String id) throws NoSuchElementException {
	DefinitionType definition = definitions.get(id);
	if (definition == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_DEFINITION, id));
	}
	return definition;
    }

    /**
     * Sort all DefinitionTypes into two lists according to whether the filter allows/disallows them.
     */
    public void filterDefinitions(IDefinitionFilter filter, Collection<DefinitionType>allow, Collection<DefinitionType>nallow) {
	for (DefinitionType dt : definitions.values()) {
	    if (filter.accept(dt.getId())) {
		allow.add(dt);
	    } else {
		nallow.add(dt);
	    }
	}
    }
}
