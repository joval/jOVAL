// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

import java.io.File;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import ocil.schemas.variables.OcilVariables;
import ocil.schemas.variables.VariableType;

import org.joval.xml.SchemaRegistry;

/**
 * Index to an OcilVariables object, for fast look-up of variable definitions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Variables {
    /**
     * Unmarshal an XML file and return the OcilVariables root object.
     */
    public static final OcilVariables getOcilVariables(File f) throws OcilException {
	return getOcilVariables(new StreamSource(f));
    }

    public static final OcilVariables getOcilVariables(Source source) throws OcilException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.OCIL));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OcilVariables) {
		return (OcilVariables)rootObj;
	    } else {
		throw new OcilException("Bad OCIL source: " + source.getSystemId());
	    }
	} catch (JAXBException e) {
	    throw new OcilException(e);
	}
    }

    private OcilVariables variables;
    private Hashtable<String, VariableType> table;

    /**
     * Create Variables from a file.
     */
    public Variables(File f) throws OcilException {
	this(getOcilVariables(f));
    }

    /**
     * Create Variables from parsed OcilVariables.
     */
    public Variables(OcilVariables variables) throws OcilException {
	this.variables = variables;
	table = new Hashtable<String, VariableType>();
	if (variables.isSetVariables() && variables.getVariables().isSetVariable()) {
	    for (VariableType var : variables.getVariables().getVariable()) {
		table.put(var.getId(), var);
	    }
	}
    }

    public OcilVariables getOcilVariables() {
	return variables;
    }

    public boolean containsVariable(String id) {
	return table.containsKey(id);
    }

    public VariableType getVariable(String id) throws NoSuchElementException {
	if (table.containsKey(id)) {
	    return table.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }
}
