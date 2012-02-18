// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import cpe.schemas.dictionary.ListType;
import xccdf.schemas.core.Benchmark;

import org.joval.intf.oval.IDefinitions;
import org.joval.oval.OvalException;
import org.joval.oval.engine.Definitions;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.xccdf.XccdfException;

/**
 * Representation of an XCCDF document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class XccdfDocument {
    static final Benchmark getBenchmark(File f) throws XccdfException {
	return getBenchmark(new StreamSource(f));
    }

    static final Benchmark getBenchmark(InputStream in) throws XccdfException {
	return getBenchmark(new StreamSource(in));
    }

    static final Benchmark getBenchmark(Source src) throws XccdfException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.XCCDF_PROP_PACKAGES);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(src);
	    if (rootObj instanceof Benchmark) {
		return (Benchmark)rootObj;
	    } else {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, src.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
    }

    private Benchmark benchmark;
    private CpeDictionary dictionary;
    private IDefinitions cpeOval, oval;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    public XccdfDocument(File f) throws XccdfException, OvalException {
	if (f.isDirectory()) {
	    File[] files = f.listFiles();
	    for (File file : files) {
		if (file.getName().toLowerCase().endsWith("xccdf.xml")) {
		    benchmark = getBenchmark(file);
		} else if (file.getName().toLowerCase().endsWith("cpe-dictionary.xml")) {
		    dictionary = new CpeDictionary(file);
		} else if (file.getName().toLowerCase().endsWith("cpe-oval.xml")) {
		    cpeOval = new Definitions(file);
		} else if (file.getName().toLowerCase().endsWith("oval.xml")) { // NB: after cpe-oval.xml
		    oval = new Definitions(file);
		}
	    }
	} else if (f.isFile()) {
	    if (f.getName().toLowerCase().endsWith(".zip")) {
		try {
		    ZipFile zip = new ZipFile(f);
		    for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
			ZipEntry entry = entries.nextElement();
			if (!entry.isDirectory()) {
			    if (entry.getName().toLowerCase().endsWith("xccdf.xml")) {
				benchmark = getBenchmark(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith("cpe-dictionary.xml")) {
				dictionary = new CpeDictionary(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith("cpe-oval.xml")) {
				cpeOval = new Definitions(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith("oval.xml")) { // NB: after cpe-oval.xml
				oval = new Definitions(zip.getInputStream(entry));
			    }
			}
		    }
		} catch (ZipException e) {
		} catch (IOException e) {
		}
	    }
	}
    }

    public CpeDictionary getDictionary() {
	return dictionary;
    }

    public IDefinitions getCpeOval() {
	return cpeOval;
    }

    public Benchmark getBenchmark() {
	return benchmark;
    }

    public IDefinitions getOval() {
	return oval;
    }
}
