// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

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

import org.slf4j.cal10n.LocLogger;

import arf.schemas.core.AssetReportCollection;
import arf.schemas.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.util.ILoggable;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.oval.Results;
import org.joval.scap.xccdf.Benchmark;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * A representation of an ARF report collection.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Report implements ILoggable, ITransformable {
    private AssetReportCollection arc;
    private LocLogger logger;
    private JAXBContext ctx;

    /**
     * Create an empty report.
     */
    public Report() throws ArfException {
	arc = Factories.core.createAssetReportCollection();

	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.ARF));
	} catch (JAXBException e) {
	    throw new ArfException(e);
	}
	logger = JOVALMsg.getLogger();
    }

    /**
     * Add an XCCDF result to the report.
     */
    public void add(Benchmark benchmark) {
    }

    /**
     * Add an OVAL result to the report.
     */
    public void add(Results results) {
    }

    /**
     * Add an OCIL checklist to the report.
     */
    public void add(Checklist checklist) {
    }

    /**
     * Get the underlying JAXB BenchmarkType.
     */
    public AssetReportCollection getAssetReportCollection() {
	return arc;
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(arc, out);
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

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(ctx, arc);
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }
}
