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
import java.util.Vector;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import scap.datastream.DataStream;
import scap.datastream.DataStreamCollection;

import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IDatastreamCollection;
import org.joval.scap.ScapException;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of an SCAP 1.2 Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DatastreamCollection implements IDatastreamCollection {
    public static final DataStreamCollection getDSCollection(File f) throws ScapException {
	return getDSCollection(new StreamSource(f));
    }

    public static final DataStreamCollection getDSCollection(InputStream in) throws ScapException {
	return getDSCollection(new StreamSource(in));
    }

    public static final DataStreamCollection getDSCollection(Source source) throws ScapException {
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.DS);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof DataStreamCollection) {
		return (DataStreamCollection)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof DataStreamCollection) {
		    return (DataStreamCollection)root.getValue();
		} else {
		    throw new ScapException("Bad Data Stream source: " + source.getSystemId());
		}
	    } else {
		throw new ScapException("Bad Data Stream source: " + source.getSystemId());
	    }
	} catch (JAXBException e) {
	    throw new ScapException(e);
	}
    }

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
	this.dsc = dsc;
	streams = new HashMap<String, DataStream>();
	for (DataStream stream : dsc.getDataStream()) {
	    streams.put(stream.getId(), stream);
	} 
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
        String packages = SchemaRegistry.lookup(SchemaRegistry.DS);
        return new JAXBSource(JAXBContext.newInstance(packages), getDSCollection());
    }

    // Implement IDatastream

    public DataStreamCollection getDSCollection() {
	return dsc;
    }

    /**
     * Return a collection of the DataStream IDs in the source document.
     */
    public Collection<String> getStreamIds() {
	return streams.keySet();
    }

    /**
     * Return a collection of Benchmark component IDs, for the given stream ID.
     */
    public IDatastream getDatastream(String id) throws NoSuchElementException, ScapException {
	if (streams.containsKey(id)) {
	    return new Datastream(streams.get(id), dsc); 
	} else {
	    throw new NoSuchElementException(id);
	}
    }
}
