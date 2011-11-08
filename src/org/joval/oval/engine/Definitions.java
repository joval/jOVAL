// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Index to an OvalDefinitions object, for fast look-up of definitions, tests, variables, objects and states.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Definitions implements IDefinitions {
    private static List<String> schematronValidationErrors = null;

    /**
     * Unmarshalls an XML file and returns the root OvalDefinitions object.
     */
    public static final OvalDefinitions getOvalDefinitions(File f) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalDefinitions) {
		return (OvalDefinitions)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_DEFINITIONS_BAD_FILE, f.toString()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private OvalDefinitions defs;
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

    public Definitions(OvalDefinitions defs) {
	this.defs = defs;

	objects = new Hashtable <String, ObjectType>();
	if (defs.getObjects() != null) {
	    List<JAXBElement <? extends ObjectType>> objectList = defs.getObjects().getObject();
	    for (JAXBElement<? extends ObjectType> jot : objectList) {
		ObjectType ot = jot.getValue();
		objects.put(ot.getId(), ot);
	    }
	}

	tests = new Hashtable <String, TestType>();
	List<JAXBElement <? extends TestType>> testList = defs.getTests().getTest();
	for (JAXBElement<? extends TestType> jtt : testList) {
	    TestType tt = jtt.getValue();
	    tests.put(tt.getId(), tt);
	}

	variables = new Hashtable <String, VariableType>();
	if (defs.getVariables() != null) {
	    List<JAXBElement <? extends VariableType>> varList = defs.getVariables().getVariable();
	    for (JAXBElement<? extends VariableType> jvt : varList) {
		VariableType vt = jvt.getValue();
		variables.put(vt.getId(), vt);
	    }
	}

	states = new Hashtable <String, StateType>();
	if (defs.getStates() != null) {
	    List<JAXBElement<? extends StateType>> stateList = defs.getStates().getState();
	    for (JAXBElement<? extends StateType> jst : stateList) {
		StateType st = jst.getValue();
		states.put(st.getId(), st);
	    }
	}

	definitions = new Hashtable <String, DefinitionType>();
	List<DefinitionType> defList = defs.getDefinitions().getDefinition();
	for (DefinitionType dt : defList) {
	    definitions.put(dt.getId(), dt);
	}
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

    public Iterator<ObjectType> iterateObjects(Class type) {
	return new SpecifiedObjectIterator(type);
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

    public Iterator <ObjectType>iterateObjects() {
	return objects.values().iterator();
    }

    public Iterator <VariableType>iterateVariables() {
	return variables.values().iterator();
    }

    // Private

    class SpecifiedObjectIterator implements Iterator<ObjectType> {
	Iterator <ObjectType>iter;
	Class type;
	ObjectType next;

	SpecifiedObjectIterator(Class type) {
	    this.type = type;
	    iter = objects.values().iterator();
	}

	public boolean hasNext() {
	    if (next != null) {
		return true;
	    }
	    try {
		next = next();
		return true;
	    } catch (NoSuchElementException e) {
	    }
	    return false;
	}

	public ObjectType next() throws NoSuchElementException {
	    if (next != null) {
		ObjectType temp = next;
		next = null;
		return temp;
	    }
	    while (true) {
		ObjectType temp = iter.next();
		if (type.isInstance(temp)) {
		    return temp;
		}
	    }
	}

	public void remove() throws UnsupportedOperationException {
	    throw new UnsupportedOperationException("remove");
	}
    }
}
