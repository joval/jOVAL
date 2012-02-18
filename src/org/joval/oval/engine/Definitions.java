// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

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

import org.slf4j.cal10n.LocLogger;

import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectsType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.StatesType;
import oval.schemas.definitions.core.TestType;
import oval.schemas.definitions.core.TestsType;
import oval.schemas.definitions.core.VariableType;
import oval.schemas.definitions.core.VariablesType;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

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
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_DEFINITIONS);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OvalDefinitions) {
		return (OvalDefinitions)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_DEFINITIONS_BAD_SOURCE, source.getSystemId()));
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
    public Definitions(File f) throws OvalException {
	this(getOvalDefinitions(f));
    }

    public Definitions(InputStream in) throws OvalException {
	this(getOvalDefinitions(in));
    }

    public Definitions(OvalDefinitions defs) {
	this.defs = defs;
	this.logger = JOVALSystem.getLogger();

	objects = new Hashtable <String, ObjectType>();
	if (defs.getObjects() != null) {
	    for (JAXBElement<? extends ObjectType> jot : defs.getObjects().getObject()) {
		ObjectType ot = jot.getValue();
		objects.put(ot.getId(), ot);
	    }
	}

	tests = new Hashtable <String, TestType>();
	for (JAXBElement<? extends TestType> jtt : defs.getTests().getTest()) {
	    TestType tt = jtt.getValue();
	    tests.put(tt.getId(), tt);
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
	for (DefinitionType dt : defs.getDefinitions().getDefinition()) {
	    definitions.put(dt.getId(), dt);
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

    public Source getSource() {
	Source src = null;
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_DEFINITIONS);
	    src = new JAXBSource(JAXBContext.newInstance(packages), getOvalDefinitions());
	} catch (JAXBException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return src;
    }

    // Implement IDefinitions

    public OvalDefinitions getOvalDefinitions() {
	return defs;
    }

    public <T extends ObjectType> T getObject(String id, Class<T> type) throws OvalException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new OvalException("Unresolved object reference ID=" + id);
	} else if (type.isInstance(object)) {
	    return type.cast(object);
	} else {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_INSTANCE, type.getName(), object.getClass().getName());
	    throw new OvalException(msg);
	}
    }

    public Collection<ObjectType> getObjects() {
	return objects.values();
    }

    public StateType getState(String id) throws OvalException {
	StateType state = states.get(id);
	if (state == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_STATE, id));
	}
	return state;
    }

    public TestType getTest(String id) throws OvalException {
	TestType test = tests.get(id);
	if (test == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_TEST, id));
	}
	return test;
    }

    public ObjectType getObject(String id) throws OvalException {
	ObjectType object = objects.get(id);
	if (object == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	return object;
    }

    public VariableType getVariable(String id) throws OvalException {
	VariableType variable = variables.get(id);
	if (variable == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_VARIABLE, id));
	} else {
	    return variable;
	}
    }

    public DefinitionType getDefinition(String id) throws OvalException {
	DefinitionType definition = definitions.get(id);
	if (definition == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_DEFINITION, id));
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
