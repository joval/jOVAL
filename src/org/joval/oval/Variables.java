// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.GeneratorType;
import oval.schemas.variables.core.OvalVariables;
import oval.schemas.variables.core.VariablesType;
import oval.schemas.variables.core.VariableType;

import org.joval.intf.util.ILoggable;
import org.joval.intf.oval.IVariables;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Index to an OvalVariables object, for fast look-up of variable definitions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Variables implements IVariables {
    /**
     * Unmarshal an XML file and return the OvalVariables root object.
     */
    public static final OvalVariables getOvalVariables(File f) throws OvalException {
	return getOvalVariables(new StreamSource(f));
    }

    public static final OvalVariables getOvalVariables(Source source) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_VARIABLES));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OvalVariables) {
		return (OvalVariables)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_VARIABLES_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private LocLogger logger;
    private GeneratorType generator;
    private OvalVariables vars;
    private JAXBContext ctx;
    private Hashtable <String, List<String>>variables;

    /**
     * Create Variables from a file.
     */
    public Variables(File f) throws OvalException {
	this(getOvalVariables(f));
    }

    /**
     * Create Variables from parsed OvalVariables.
     */
    public Variables(OvalVariables vars) throws OvalException {
	this();
	this.vars = vars;
	List <VariableType> varList = vars.getVariables().getVariable();
	int len = varList.size();
	for (int i=0; i < len; i++) {
	    VariableType vt = varList.get(i);
	    variables.put(vt.getId(), extractValue(vt));
	}
    }

    /**
     * Create empty Variables.
     */
    public Variables(GeneratorType generator) {
	this();
	this.generator = generator;
    }

    public void setValue(String id, List<String> value) {
	variables.put(id, value);
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getOvalVariables(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		}
	    }
	}
    }

    public OvalVariables getOvalVariables() {
	if (vars == null) {
	    vars = createOvalVariables();
	}
	return vars;
    }

    // Implement IVariables

    public List<String> getValue(String id) throws NoSuchElementException {
	List<String> values = variables.get(id);
	if (values == null) {
	    throw new NoSuchElementException(id);
	} else {
	    return values;
	}
    }

    // Implement ITransformable

    public Source getSource() {
	Source src = null;
	try {
	    src = new JAXBSource(ctx, getOvalVariables());
	} catch (JAXBException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return src;
    }

    // Implement ILogger

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    private Variables() {
	logger = JOVALSystem.getLogger();
	variables = new Hashtable<String, List<String>>();
	try {
	    ctx = JAXBContext.newInstance(JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_VARIABLES));
	} catch (JAXBException e) {
	    logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private OvalVariables createOvalVariables() {
	OvalVariables vars = JOVALSystem.factories.variables.createOvalVariables();
	vars.setGenerator(generator);

	VariablesType vt = JOVALSystem.factories.variables.createVariablesType();
	for (String key : variables.keySet()) {
	    VariableType var = JOVALSystem.factories.variables.createVariableType();
	    var.setId(key);
	    for (String s : variables.get(key)) {
		var.getValue().add(s);
	    }
	    vt.getVariable().add(var);
	}

	vars.setVariables(vt);
	return vars;
    }

    /**
     * Reads String (i.e., Text) data from the VariableType as a Node.
     */
    private List<String> extractValue(VariableType var) throws OvalException {
	List<String> list = new Vector<String>();
	for (Object obj : var.getValue()) {
	    if (obj instanceof Node) {
		//
		// xsi:type was unspecified
		//
		list.add(((Node)obj).getTextContent());
	    } else {
		list.add(obj.toString());
	    }
	}
	return list;
    }
}
