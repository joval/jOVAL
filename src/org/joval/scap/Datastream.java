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
import xccdf.schemas.core.BenchmarkType;

import org.joval.cpe.CpeException;
import org.joval.cpe.Dictionary;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;
import org.joval.oval.Definitions;
import org.joval.scap.ScapException;
import org.joval.util.JOVALMsg;
import org.joval.xccdf.Benchmark;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of an SCAP 1.2 Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Datastream implements ILoggable {
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

    /**
     * Return a collection of the DataStream IDs in the source document.
     */
    public Collection<String> getStreamIds() {
	return streams.keySet();
    }

    /**
     * Return a collection of Benchmark component IDs, for the given stream ID.
     */
    public Collection<String> getBenchmarkIds(String streamId) throws NoSuchElementException {
	if (streams.containsKey(streamId)) {
	    Collection<String> benchmarkIds = new Vector<String>();
	    for (ComponentRef ref : streams.get(streamId).getChecklists().getComponentRef()) {
		String href = ref.getHref();
		if (href.startsWith("#")) {
		    href = href.substring(1);
		}
		benchmarkIds.add(href);
	    }
	    return benchmarkIds; 
	} else {
	    throw new NoSuchElementException(streamId);
	}
    }

    public Benchmark getBenchmark(String streamId, String benchmarkId) throws NoSuchElementException, ScapException {
	if (streams.containsKey(streamId)) {
	    if (components.containsKey(benchmarkId)) {
		Component comp = components.get(benchmarkId);
		if (comp.isSetBenchmark()) {
		    Dictionary dictionary = null;
		    try {
			dictionary = getDictionary(streamId);
		    } catch (NoSuchElementException e) {
			logger.warn("WARNING - dictionary not found: " + e.getMessage());
		    }
		    return new Benchmark(streamId, comp.getId(), this, comp.getBenchmark(), dictionary);
		} else {
		    throw new NoSuchElementException("Not a benchmark component: " + benchmarkId);
		}
	    } else {
		throw new NoSuchElementException(benchmarkId);
	    }
	} else {
	    throw new NoSuchElementException(streamId);
	}
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
	dictionaries = new Hashtable<String, Dictionary>();
	logger = JOVALMsg.getLogger();
    }

    private Dictionary getDictionary(String streamId) throws NoSuchElementException, CpeException {
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
		logger.warn("ERROR: Multiple dictionaries specified in stream " + streamId);
	    }
	}
	throw new NoSuchElementException(streamId);
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
