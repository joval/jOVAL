// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.cal10n.LocLogger;

import cpe.schemas.dictionary.ListType;
import xccdf.schemas.core.Benchmark;

import org.joval.cpe.CpeException;
import org.joval.cpe.Dictionary;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.util.ILoggable;
import org.joval.oval.Definitions;
import org.joval.oval.OvalException;
import org.joval.protocol.zip.ZipURLStreamHandler;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.xccdf.XccdfException;

/**
 * Representation of an XCCDF document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XccdfBundle implements ILoggable {
    public static final String CPE_DICTIONARY	= "cpe-dictionary.xml";
    public static final String CPE_OVAL		= "cpe-oval.xml";
    public static final String XCCDF_BENCHMARK	= "xccdf.xml";
    public static final String XCCDF_OVAL	= "oval.xml";

    public static final Benchmark getBenchmark(File f) throws XccdfException {
	return getBenchmark(new StreamSource(f));
    }

    public static final Benchmark getBenchmark(InputStream in) throws XccdfException {
	return getBenchmark(new StreamSource(in));
    }

    public static final Benchmark getBenchmark(Source src) throws XccdfException {
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

    private LocLogger logger;
    private Benchmark benchmark;
    private Dictionary dictionary;
    private IDefinitions cpeOval, oval;
    private File base;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    public XccdfBundle(File f) throws CpeException, OvalException, XccdfException {
	base = f;
	logger = JOVALSystem.getLogger();
	if (f.isDirectory()) {
	    File[] files = f.listFiles();
	    for (File file : files) {
		if (file.getName().toLowerCase().endsWith(XCCDF_BENCHMARK)) {
		    benchmark = getBenchmark(file);
		} else if (file.getName().toLowerCase().endsWith(CPE_DICTIONARY)) {
		    dictionary = new Dictionary(file);
		} else if (file.getName().toLowerCase().endsWith(CPE_OVAL)) {
		    cpeOval = new Definitions(file);
		} else if (file.getName().toLowerCase().endsWith(XCCDF_OVAL)) { // NB: after cpe-oval.xml
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
			    if (entry.getName().toLowerCase().endsWith(XCCDF_BENCHMARK)) {
				benchmark = getBenchmark(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith(CPE_DICTIONARY)) {
				dictionary = new Dictionary(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith(CPE_OVAL)) {
				cpeOval = new Definitions(zip.getInputStream(entry));
			    } else if (entry.getName().toLowerCase().endsWith(XCCDF_OVAL)) { // NB: after cpe-oval.xml
				oval = new Definitions(zip.getInputStream(entry));
			    }
			}
		    }
		} catch (ZipException e) {
		    throw new XccdfException(e);
		} catch (IOException e) {
		    throw new XccdfException(e);
		}
	    }
	}
	if (dictionary == null) {
	    throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, CPE_DICTIONARY));
	}
	if (cpeOval == null) {
	    throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, CPE_OVAL));
	}
	if (benchmark == null) {
	    throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, XCCDF_BENCHMARK));
	}
	if (oval == null) {
	    throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, XCCDF_OVAL));
	}
    }

    /**
     * Given an HREF (which may be relative or absolute), return a URL to the resource.
     */
    public URL getURL(String href) throws MalformedURLException, SecurityException {
	try {
	    return new URL(href);
	} catch (MalformedURLException e) {
	    if (base.isDirectory()) {
		return new URL(base.toURI().toURL(), href);
	    } else if (base.isFile() && base.getName().endsWith(".zip")) {
		StringBuffer spec = new StringBuffer("zip:");
		spec.append(base.toURI().toURL().toString());
		spec.append("!/");
		spec.append(href);
		return new URL(null, spec.toString(), new ZipURLStreamHandler());
	    } else {
		throw e;
	    }
	}
    }

    public Dictionary getDictionary() {
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

    public void writeBenchmarkXML(File f) {
	OutputStream out = null;
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.XCCDF_PROP_PACKAGES);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getBenchmark(), out);
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

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }
}
