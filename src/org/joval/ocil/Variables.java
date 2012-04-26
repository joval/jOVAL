// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import ocil.schemas.variables.OcilVariables;

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

    /**
     * Create Variables from a file.
     */
    Variables(File f) throws OcilException {
	this(getOcilVariables(f));
    }

    /**
     * Create Variables from parsed OcilVariables.
     */
    Variables(OcilVariables variables) throws OcilException {
	this.variables = variables;
    }

    public OcilVariables getOcilVariables() {
	return variables;
    }
}
