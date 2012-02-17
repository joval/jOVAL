// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import xccdf.schemas.core.Benchmark;

import org.joval.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Representation of an XCCDF document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class XccdfDocument {
    /**
     * Get the root-level node (Benchmark) of the document.
     */
    static final Benchmark getBenchmark(File f) throws XccdfException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.XCCDF_PROP_PACKAGES);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof Benchmark) {
		return (Benchmark)rootObj;
	    } else {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_BAD_FILE, f.toString()));
	    }
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
    }

    private Benchmark benchmark;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    XccdfDocument(File f) throws XccdfException {
	this(getBenchmark(f));
    }

    /**
     * Create a Directives from unmarshalled XML.
     */
    XccdfDocument(Benchmark benchmark) {
	this.benchmark = benchmark;
    }

    Benchmark getBenchmark() {
	return benchmark;
    }
}
