// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import scap.ocil.core.VariableDataType;
import scap.ocil.variables.OcilVariables;
import scap.ocil.variables.VariablesType;
import scap.ocil.variables.VariableType;

import org.joval.intf.scap.ocil.IVariables;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

/**
 * Index to an OcilVariables object, for fast look-up of variable definitions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Variables implements IVariables {
    /**
     * Unmarshal an XML file and return the OcilVariables root object.
     */
    public static final OcilVariables getOcilVariables(File f) throws OcilException {
	return getOcilVariables(new StreamSource(f));
    }

    public static final OcilVariables getOcilVariables(Source source) throws OcilException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OCIL.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OcilVariables) {
		return (OcilVariables)rootObj;
	    } else {
		throw new OcilException(JOVALMsg.getMessage(JOVALMsg.ERROR_OCIL_BAD_SOURCE, source.getSystemId()));
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
     * Create Variables from a file.
     */
    public Variables(InputStream in) throws OcilException {
	this(getOcilVariables(new StreamSource(in)));
    }

    /**
     * Create Variables from parsed OcilVariables.
     */
    public Variables(OcilVariables variables) throws OcilException {
	this();
	this.variables = variables;
	if (variables.isSetVariables() && variables.getVariables().isSetVariable()) {
	    for (VariableType var : variables.getVariables().getVariable()) {
		table.put(var.getId(), var);
	    }
	}
    }

    /**
     * Create an empty Variables structure.
     */
    public Variables() throws OcilException {
	table = new Hashtable<String, VariableType>();
    }

    public void addValue(String id, String value) {
	addValue(id, VariableDataType.TEXT, value);
    }

    public void addValue(String id, VariableDataType type, String value) {
	if (variables != null) {
	    variables = null;
	}
	VariableType var = Factories.variables.createVariableType();
	var.setId(id);
	var.setDatatype(type);
	var.setValue(value);
	table.put(id, var);
    }

    public void setComment(String id, String comment) throws NoSuchElementException {
	if (table.containsKey(id)) {
	    table.get(id).setComment(comment);
	} else {
	    throw new NoSuchElementException(id);
	}
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

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    out = new FileOutputStream(f);
	    writeXML(out);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    public void writeXML(OutputStream out) throws IOException {
	try {
	    Marshaller marshaller = SchemaRegistry.OCIL.createMarshaller();
	    marshaller.marshal(getRootObject(), out);
	} catch (JAXBException e) {
	    throw new IOException(e);
	} catch (FactoryConfigurationError e) {
	    throw new IOException(e);
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.OCIL.getJAXBContext(), getRootObject());
    }

    public OcilVariables getRootObject() {
	if (variables == null) {
	    variables = Factories.variables.createOcilVariables();
	    variables.setGenerator(Factories.getGenerator());
	    VariablesType vars = Factories.variables.createVariablesType();
	    for (String id : table.keySet()) {
		vars.getVariable().add(table.get(id));
	    }
	    variables.setVariables(vars);
	}
	return variables;
    }

    public OcilVariables copyRootObject() throws Exception {
        Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
        Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
        if (rootObj instanceof OcilVariables) {
            return (OcilVariables)rootObj;
        } else {
            throw new OcilException(JOVALMsg.getMessage(JOVALMsg.ERROR_OCIL_BAD_SOURCE, toString()));
        }
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OCIL.getJAXBContext();
    }
}
