// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
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
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
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
    private Map<String, DefinitionType> definitions;
    private Map<String, JAXBElement<? extends TestType>> tests;
    private Map<String, JAXBElement<? extends StateType>> states;
    private Map<String, VariableType> variables;
    private Map<String, JAXBElement<? extends ObjectType>> objects;

    /**
     * Load OVAL definitions from a File.
     */
    Definitions(File f) throws OvalException {
	this(getOvalDefinitions(f));
    }

    Definitions(InputStream in) throws OvalException {
	this(getOvalDefinitions(in));
    }

    public Definitions(OvalDefinitions defs) {
	this();
	this.defs = defs;
	if (defs.isSetObjects() && defs.getObjects().isSetObject()) {
	    for (JAXBElement<? extends ObjectType> jot : defs.getObjects().getObject()) {
		objects.put(jot.getValue().getId(), jot);
	    }
	}

	if (defs.isSetTests() && defs.getTests().isSetTest()) {
	    for (JAXBElement<? extends TestType> jtt : defs.getTests().getTest()) {
		tests.put(jtt.getValue().getId(), jtt);
	    }
	}

	if (defs.isSetVariables() && defs.getVariables().isSetVariable()) {
	    for (JAXBElement<? extends VariableType> jvt : defs.getVariables().getVariable()) {
		VariableType vt = jvt.getValue();
		variables.put(vt.getId(), vt);
	    }
	}

	if (defs.isSetStates() && defs.getStates().isSetState()) {
	    for (JAXBElement<? extends StateType> jst : defs.getStates().getState()) {
		states.put(jst.getValue().getId(), jst);
	    }
	}

	if (defs.isSetDefinitions() && defs.getDefinitions().isSetDefinition()) {
	    for (DefinitionType dt : defs.getDefinitions().getDefinition()) {
		definitions.put(dt.getId(), dt);
	    }
	}
    }

    public Definitions() {
	defs = Factories.definitions.core.createOvalDefinitions();
	this.logger = JOVALMsg.getLogger();
	objects = new HashMap<String, JAXBElement<? extends ObjectType>>();
	tests = new HashMap<String, JAXBElement<? extends TestType>>();
	variables = new HashMap<String, VariableType>();
	states = new HashMap<String, JAXBElement<? extends StateType>>();
	definitions = new HashMap<String, DefinitionType>();
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
	return new JAXBSource(SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext(), getRootObject());
    }

    public OvalDefinitions getRootObject() {
	return defs; 
    }

    public OvalDefinitions copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof OvalDefinitions) {
	    return (OvalDefinitions)rootObj;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITIONS_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext();
    }

    // Implement IDefinitions

    public <T extends ObjectType> T getObject(String id, Class<T> type) throws NoSuchElementException {
	if (objects.containsKey(id)) {
	    ObjectType object = objects.get(id).getValue();
	    if (type.isInstance(object)) {
		return type.cast(object);
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_INSTANCE, type.getName(), object.getClass().getName());
		throw new NoSuchElementException(msg);
	    }
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
    }

    public Collection<ObjectType> getObjects() {
	ArrayList<ObjectType> result = new ArrayList<ObjectType>();
	for (JAXBElement<? extends ObjectType> elt : objects.values()) {
	    result.add(elt.getValue());
	}
	return result;
    }

    public Collection<VariableType> getVariables() {
	return Collections.unmodifiableCollection(variables.values());
    }

    public JAXBElement<? extends StateType> getState(String id) throws NoSuchElementException {
	if (states.containsKey(id)) {
	    return states.get(id);
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_STATE, id));
    }

    public JAXBElement<? extends TestType> getTest(String id) throws NoSuchElementException {
	if (tests.containsKey(id)) {
	    return tests.get(id);
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_TEST, id));
    }

    public JAXBElement<? extends ObjectType> getObject(String id) throws NoSuchElementException {
	if (objects.containsKey(id)) {
	    return objects.get(id);
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
    }

    public VariableType getVariable(String id) throws NoSuchElementException {
	if (variables.containsKey(id)) {
	    return variables.get(id);
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_VARIABLE, id));
    }

    public DefinitionType getDefinition(String id) throws NoSuchElementException {
	if (definitions.containsKey(id)) {
	    return definitions.get(id);
	}
	throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_DEFINITION, id));
    }
}
