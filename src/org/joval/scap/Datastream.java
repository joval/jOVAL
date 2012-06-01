// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
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

import org.slf4j.cal10n.LocLogger;

import scap.datastream.Component;
import scap.datastream.ComponentRef;
import scap.datastream.ContentSourceType;
import scap.datastream.DataStream;
import scap.datastream.DataStreamCollection;
import scap.datastream.ExtendedComponent;
import scap.datastream.RefListType;
import scap.datastream.UseCaseType;
import org.oasis.catalog.Catalog;
import org.oasis.catalog.Uri;
import xccdf.schemas.core.Benchmark;

import org.joval.cpe.CpeException;
import org.joval.cpe.Dictionary;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.util.ILoggable;
import org.joval.intf.xml.ITransformable;
import org.joval.oval.OvalException;
import org.joval.oval.Definitions;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of an SCAP 1.2 Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Datastream implements ITransformable, ILoggable {
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

    private LocLogger logger;
    private JAXBContext ctx;
    private DataStreamCollection dsc;
    private Hashtable<String, DataStream> streams;
    private Hashtable<String, StreamResolver> resolvers;
    private Hashtable<String, Component> components;
    private Hashtable<String, Dictionary> dictionaries;
    private Hashtable<String, Benchmark> benchmarks;

    /**
     * Create a Datastream based on the contents of a checklist file.
     */
    public Datastream(File f) throws ScapException {
	this(getDSCollection(f));
    }

    public Datastream(InputStream in) throws ScapException {
	this(getDSCollection(in));
    }

    /**
     * Create a Datastream from unmarshalled XML.
     */
    public Datastream(DataStreamCollection dsc) throws ScapException {
	this();
	this.dsc = dsc;
	for (Component component : dsc.getComponent()) {
	    components.put(component.getId(), component);
	}
	for (DataStream stream : dsc.getDataStream()) {
	    streams.put(stream.getId(), stream);
	    resolvers.put(stream.getId(), new StreamResolver(stream));
	} 
    }

    public DataStreamCollection getDSCollection() {
	return dsc;
    }

    public Collection<String> getStreamIds() {
	return resolvers.keySet();
    }

    public Benchmark getBenchmark(String streamId) throws NoSuchElementException {
	if (benchmarks.containsKey(streamId)) {
	    return benchmarks.get(streamId);
	} else if (streams.containsKey(streamId)) {
	    RefListType refs = streams.get(streamId).getChecklists();
	    if (refs.getComponentRef().size() == 1) {
		String componentHref = refs.getComponentRef().get(0).getHref();
		if (componentHref.startsWith("#")) {
		    componentHref = componentHref.substring(1);
		}
		if (components.containsKey(componentHref)) {
		    Benchmark b = components.get(componentHref).getBenchmark();
		    benchmarks.put(streamId, b);
		    return b;
		} else {
		    throw new NoSuchElementException(componentHref);
		}
	    } else if (refs.getComponentRef().size() > 1) {
		System.out.println("ERROR: Multiple Benchmarks specified in stream " + streamId);
	    }
	}
	throw new NoSuchElementException(streamId);
    }

    public Dictionary getDictionary(String streamId) throws NoSuchElementException, CpeException {
	if (dictionaries.containsKey(streamId)) {
	    return dictionaries.get(streamId);
	} else if (streams.containsKey(streamId)) {
	    RefListType refs = streams.get(streamId).getDictionaries();
	    if (refs.getComponentRef().size() == 1) {
		String dictionaryId = refs.getComponentRef().get(0).getHref();
		if (dictionaryId.startsWith("#")) {
		    dictionaryId = dictionaryId.substring(1);
		}
		if (components.containsKey(dictionaryId)) {
		    Dictionary d = new Dictionary(components.get(dictionaryId).getCpeList());
		    dictionaries.put(streamId, d);
		    return d;
		} else {
		    throw new NoSuchElementException(dictionaryId);
		}
	    } else if (refs.getComponentRef().size() > 1) {
		System.out.println("ERROR: Multiple dictionaries specified in stream " + streamId);
	    }
	}
	throw new NoSuchElementException(streamId);
    }

    public IDefinitions getDefinitions(String streamId, String href) throws NoSuchElementException, OvalException {
	if (resolvers.containsKey(streamId)) {
	    Component component = resolvers.get(streamId).resolve(href);
	    if (component.isSetOvalDefinitions()) {
		return new Definitions(component.getOvalDefinitions());
	    } else {
		throw new NoSuchElementException(href);
	    }
	} else {
	    throw new NoSuchElementException(streamId);
	}
    }

    public void writeBenchmarkXML(String streamId, File f) throws NoSuchElementException {
	OutputStream out = null;
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.XCCDF);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getBenchmark(streamId), out);
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
	return new JAXBSource(ctx, getDSCollection());
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    /**
     * Create an empty Datastream.
     */
    private Datastream() throws ScapException {
	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.OCIL));
	} catch (JAXBException e) {
	    throw new ScapException(e);
	}
	components = new Hashtable<String, Component>();
	resolvers = new Hashtable<String, StreamResolver>();
	streams = new Hashtable<String, DataStream>();
	benchmarks = new Hashtable<String, Benchmark>();
	dictionaries = new Hashtable<String, Dictionary>();
	logger = JOVALMsg.getLogger();
    }

    /**
     * A class that relates XCCDF check hrefs to Components.
     */
    class StreamResolver {
	private Hashtable<String, Component> map;

	StreamResolver(DataStream stream) {
	    String id = stream.getId();
	    Hashtable<String, String> checks = new Hashtable<String, String>();
	    for (ComponentRef ref : stream.getChecks().getComponentRef()) {
		String href = ref.getHref();
		if (href.startsWith("#")) {
		    href = href.substring(1);
		}
		checks.put(ref.getId(), href);
	    }
	    map = new Hashtable<String, Component>();
	    for (ComponentRef ref : stream.getDictionaries().getComponentRef()) {
		if (ref.isSetCatalog()) {
		    addCatalog(ref.getCatalog(), checks);
		}
	    }
	    for (ComponentRef ref : stream.getChecklists().getComponentRef()) {
		if (ref.isSetCatalog()) {
		    addCatalog(ref.getCatalog(), checks);
		}
	    }
	}

	/**
	 * Get the component corresponding to the specified href in this stream.
	 */
	Component resolve(String href) throws NoSuchElementException {
	    if (map.containsKey(href)) {
		return map.get(href);
	    } else {
		throw new NoSuchElementException(href);
	    }
	}

	/**
	 * Add a catalog to the resolver.
	 */
	private void addCatalog(Catalog catalog, Hashtable<String, String> checks) {
	    for (Object obj : catalog.getPublicOrSystemOrUri()) {
		if (obj instanceof JAXBElement) {
		    JAXBElement elt = (JAXBElement)obj;
		    if (elt.getValue() instanceof Uri) {
			Uri u = (Uri)elt.getValue();
			String uri = u.getUri();
			if (uri.startsWith("#")) {
			    uri = uri.substring(1);
			}
			if (checks.containsKey(uri)) {
			    String componentId = checks.get(uri);
			    if (components.containsKey(componentId)) {
				if (map.containsKey(u.getName())) {
				    System.out.println("ERROR: Duplicate URI name: " + u.getName());
				} else {
				    map.put(u.getName(), components.get(componentId));
				}
			    } else {
				System.out.println("ERROR: No component found for id " + componentId);
			    }
			} else {
			    System.out.println("ERROR: No check found for catalog URI " + uri);
			}
		    } else if (elt.isNil()) {
			System.out.println("ERROR: Nil element");
		    } else {
			System.out.println("ERROR: Not a Uri: " + elt.getValue().getClass().getName());
		    }
		}
	    }
	}
    }
}
