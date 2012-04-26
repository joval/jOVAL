// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import ocil.schemas.core.OCILType;

import org.joval.xml.SchemaRegistry;

/**
 * Representation of a OCIL checklist document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checklist {
    public static final OCILType getOCILType(File f) throws OcilException {
	return getOCILType(new StreamSource(f));
    }

    public static final OCILType getOCILType(InputStream in) throws OcilException {
	return getOCILType(new StreamSource(in));
    }

    public static final OCILType getOCILType(Source source) throws OcilException {
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.OCIL);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OCILType) {
		return (OCILType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof OCILType) {
		    return (OCILType)root.getValue();
		} else {
		    throw new OcilException("Bad OCIL source: " + source.getSystemId());
		}
	    } else {
		throw new OcilException("Bad OCIL source: " + source.getSystemId());
	    }
	} catch (JAXBException e) {
	    throw new OcilException(e);
	}
    }

    private OCILType ocil;

    /**
     * Create a Checklist based on the contents of a checklist file.
     */
    public Checklist(File f) throws OcilException {
	this(getOCILType(f));
    }

    public Checklist(InputStream in) throws OcilException {
	this(getOCILType(in));
    }

    /**
     * Create a Checklist from unmarshalled XML.
     */
    public Checklist(OCILType ocil) {
	this.ocil = ocil;
    }

    public OCILType getOCILType() {
	return ocil;
    }
}
