// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.datastream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.datastream.DataStream;
import scap.datastream.DataStreamCollection;

import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IDatastreamCollection;
import org.joval.scap.ScapException;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of an SCAP 1.2 Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DatastreamCollection implements IDatastreamCollection, ILoggable {
    public static final DataStreamCollection getDSCollection(File f) throws ScapException {
	return getDSCollection(new StreamSource(f));
    }

    public static final DataStreamCollection getDSCollection(InputStream in) throws ScapException {
	return getDSCollection(new StreamSource(in));
    }

    public static final DataStreamCollection getDSCollection(Source source) throws ScapException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.DS.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof DataStreamCollection) {
		return (DataStreamCollection)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof DataStreamCollection) {
		    return (DataStreamCollection)root.getValue();
		} else {
		    throw new ScapException(JOVALMsg.getMessage(JOVALMsg.ERROR_DATASTREAM_BAD_SOURCE, source.getSystemId()));
		}
	    } else {
		throw new ScapException(JOVALMsg.getMessage(JOVALMsg.ERROR_DATASTREAM_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new ScapException(e);
	}
    }

    private LocLogger logger;
    private DataStreamCollection dsc;
    private Map<String, DataStream> streams;

    /**
     * Create a Datastream collection based on the contents of a file.
     */
    public DatastreamCollection(File f) throws ScapException {
	this(getDSCollection(f));
    }

    public DatastreamCollection(InputStream in) throws ScapException {
	this(getDSCollection(in));
    }

    public DatastreamCollection(Source src) throws ScapException {
	this(getDSCollection(src));
    }

    /**
     * Create a Datastream collection from unmarshalled XML.
     */
    public DatastreamCollection(DataStreamCollection dsc) throws ScapException {
	logger = JOVALMsg.getLogger();
	this.dsc = dsc;
	streams = new HashMap<String, DataStream>();
	for (DataStream stream : dsc.getDataStream()) {
	    streams.put(stream.getId(), stream);
	} 
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.DS.getJAXBContext(), getRootObject());
    }

    public DataStreamCollection getRootObject() {
	return dsc;
    }

    public DataStreamCollection copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof DataStreamCollection) {
	    return (DataStreamCollection)rootObj;
	} else {
	    throw new ScapException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESULTS_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.DS.getJAXBContext();
    }

    // Implement IDatastreamCollection

    public Collection<String> getStreamIds() {
	return streams.keySet();
    }

    public IDatastream getDatastream(String id) throws NoSuchElementException, ScapException {
	if (streams.containsKey(id)) {
	    return new Datastream(streams.get(id), dsc); 
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.DS.createMarshaller();
	    out = new FileOutputStream(f);
	    marshaller.marshal(getRootObject(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE,  e.toString());
		}
	    }
	}
    }
}
