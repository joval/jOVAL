// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
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

import org.slf4j.cal10n.LocLogger;

import xccdf.schemas.core.BenchmarkType;
import xccdf.schemas.core.ProfileType;
import org.openscap.sce.xccdf.ScriptDataType;

import org.joval.cpe.CpeException;
import org.joval.cpe.Dictionary;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.util.ILoggable;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.Datastream;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of a single XCCDF 1.2 benchmark sourced from an SCAP 1.2 DataStreamCollection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Benchmark implements ILoggable, ITransformable {
    private String streamId, componentId;
    private Datastream ds;
    private LocLogger logger;
    private JAXBContext ctx;
    private BenchmarkType bt;
    private Dictionary dictionary;
    private Hashtable<String, ProfileType> profiles;

    public Benchmark(String streamId, String componentId, Datastream ds, BenchmarkType bt, Dictionary dictionary)
		throws XccdfException {

	this.streamId = streamId;
	this.componentId = componentId;
	this.ds = ds;
	this.bt = bt;
	this.dictionary = dictionary;

	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.XCCDF));
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
	logger = ds.getLogger();
	profiles = new Hashtable<String, ProfileType>();
	for (ProfileType profile : bt.getProfile()) {
	    profiles.put(profile.getProfileId(), profile);
	}
    }

    /**
     * Get the underlying JAXB BenchmarkType.
     */
    public BenchmarkType getBenchmark() {
	return bt;
    }

    public String getHref() {
	return "#" + componentId;
    }

    public Collection<String> getProfileIds() {
	return profiles.keySet();
    }

    public Dictionary getDictionary() {
	return dictionary;
    }

    public IDefinitions getDefinitions(String href) throws OvalException, NoSuchElementException {
	return ds.getDefinitions(streamId, href);
    }

    public ScriptDataType getScript(String href) throws NoSuchElementException {
	return ds.getScript(streamId, href);
    }

    public void writeBenchmarkXML(File f) throws NoSuchElementException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(bt, out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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

    /**
     * Transform using the specified template, and serialize to the specified file.
     */
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
