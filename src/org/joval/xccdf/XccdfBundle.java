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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
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
    private String cpeOvalHref=null, ovalHref=null;

    /**
     * Create a Directives based on the contents of a directives file.
     */
    public XccdfBundle(File f) throws CpeException, OvalException, XccdfException {
	base = f;
	logger = JOVALSystem.getLogger();
	try {
	    List<String> fnames = null;
	    if (f.isDirectory()) {
		fnames = Arrays.asList(f.list());
	    } else if (f.isFile() && f.getName().toLowerCase().endsWith(".zip")) {
		List<String> list = new Vector<String>();
		ZipFile zip = new ZipFile(f);
		for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
		    ZipEntry entry = entries.nextElement();
		    if (!entry.isDirectory()) {
			list.add(entry.getName());
		    }
		}
	    }

	    String benchmarkHref=null, dictionaryHref=null;
	    if (fnames != null) {
		for (String fname : fnames) {
		    if (fname.toLowerCase().endsWith(XCCDF_BENCHMARK)) {
			benchmarkHref = fname;
		    } else if (fname.toLowerCase().endsWith(CPE_DICTIONARY)) {
			dictionaryHref = fname;
		    } else if (fname.toLowerCase().endsWith(CPE_OVAL)) {
			cpeOvalHref = fname;
		    } else if (fname.toLowerCase().endsWith(XCCDF_OVAL)) { // NB: after cpe-oval.xml
			ovalHref = fname;
		    }
		}
	    }

	    if (benchmarkHref == null) {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, XCCDF_BENCHMARK));
	    } else {
		logger.info(JOVALMsg.STATUS_XCCDF_BENCHMARK, benchmarkHref);
		benchmark = getBenchmark(getURL(benchmarkHref).openStream());
	    }
	    if (dictionaryHref == null) {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, CPE_DICTIONARY));
	    } else {
		logger.info(JOVALMsg.STATUS_XCCDF_DICTIONARY, dictionaryHref);
		dictionary = new Dictionary(getURL(dictionaryHref).openStream());
	    }
	    if (cpeOvalHref == null) {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, CPE_OVAL));
	    } else {
		logger.info(JOVALMsg.STATUS_XCCDF_PLATFORM, cpeOvalHref);
		cpeOval = new Definitions(getURL(cpeOvalHref).openStream());
	    }
	    if (ovalHref == null) {
		throw new XccdfException(JOVALSystem.getMessage(JOVALMsg.ERROR_XCCDF_MISSING_PART, XCCDF_OVAL));
	    } else {
		logger.info(JOVALMsg.STATUS_XCCDF_OVAL, ovalHref);
		oval = new Definitions(getURL(ovalHref).openStream());
	    }
	} catch (IOException e) {
	    throw new XccdfException(e);
	}
    }

    /**
     * Given an HREF (which may either be relative to this bundle or absolute), return a URL to the resource.
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

    public String getCpeOvalHref() {
	return cpeOvalHref;
    }

    public String getOvalHref() {
	return ovalHref;
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
