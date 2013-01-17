// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.datastream.Component;
import scap.xccdf.BenchmarkType;

import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of an XCCDF 1.2 benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Benchmark implements IBenchmark, ILoggable {
    public static final BenchmarkType getBenchmarkType(File f) throws XccdfException {
	return getBenchmarkType(new StreamSource(f));
    }

    public static final BenchmarkType getBenchmarkType(InputStream in) throws XccdfException {
	return getBenchmarkType(new StreamSource(in));
    }

    public static final BenchmarkType getBenchmarkType(Source source) throws XccdfException {
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.XCCDF);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof BenchmarkType) {
		return (BenchmarkType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof BenchmarkType) {
		    return (BenchmarkType)root.getValue();
		} else {
		    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, source.getSystemId()));
		}
	    } else {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
    }

    private LocLogger logger;
    private JAXBContext ctx;
    private BenchmarkType bt;
    private String href;

    Benchmark() throws XccdfException {
	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.XCCDF));
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
	logger = JOVALMsg.getLogger();
    }

    public Benchmark(BenchmarkType bt) throws XccdfException {
	this();
	this.bt = bt;
    }

    public Benchmark(Component component) throws XccdfException {
	this();
	if (component.isSetBenchmark()) {
	    bt = component.getBenchmark();
	    href = "#" + component.getId();
	} else {
	    throw new XccdfException("Not a benchmark component: " + component.getId());
	}
    }

    // Implement IBenchmark

    public BenchmarkType getBenchmark() {
	return bt;
    }

    public String getHref() {
	return href;
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(bt, out);
	} catch (JAXBException e) {
	    throw new IOException(e);
	} catch (FactoryConfigurationError e) {
	    throw new IOException(e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    public void writeTransform(File transform, File output) {
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(transform)));
	    transformer.transform(getSource(), new StreamResult(output));
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerConfigurationException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(ctx, bt);
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }
}
