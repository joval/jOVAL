// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
    private LocLogger logger;
    private JAXBContext ctx;
    private BenchmarkType bt;
    private String href;

    public Benchmark(Component component) throws XccdfException {
	if (component.isSetBenchmark()) {
	    bt = component.getBenchmark();
	    href = "#" + component.getId();
	    try {
		ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.XCCDF));
	    } catch (JAXBException e) {
		throw new XccdfException(e);
	    }
	    logger = JOVALMsg.getLogger();
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
