// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.cpe;

import java.io.File;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import cpe.schemas.dictionary.ListType;

import org.joval.cpe.CpeException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Representation of a CPE dictionary document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Dictionary {
    public static final ListType getCpeList(File f) throws CpeException {
	return getCpeList(new StreamSource(f));
    }

    public static final ListType getCpeList(InputStream in) throws CpeException {
	return getCpeList(new StreamSource(in));
    }

    public static final ListType getCpeList(Source source) throws CpeException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.CPE_PROP_PACKAGES);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof ListType) {
		return (ListType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof ListType) {
		    return (ListType)root.getValue();
		} else {
		    throw new CpeException(JOVALSystem.getMessage(JOVALMsg.ERROR_CPE_BAD_SOURCE, source.getSystemId()));
		}
	    } else {
		throw new CpeException(JOVALSystem.getMessage(JOVALMsg.ERROR_CPE_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new CpeException(e);
	}
    }

    private ListType list;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    public Dictionary(File f) throws CpeException {
	this(getCpeList(f));
    }

    public Dictionary(InputStream in) throws CpeException {
	this(getCpeList(in));
    }

    /**
     * Create a Directives from unmarshalled XML.
     */
    public Dictionary(ListType list) {
	this.list = list;
    }

    public ListType getCpeList() {
	return list;
    }
}
