// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.datastream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBElement;

import jsaf.intf.util.ILoggable;
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
import org.openscap.sce.xccdf.ScriptDataType;
import scap.xccdf.BenchmarkType;
import scap.xccdf.ProfileType;

import org.joval.intf.scap.cpe.IDictionary;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IView;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.scap.ScapException;
import org.joval.scap.cpe.Dictionary;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.Definitions;
import org.joval.scap.oval.OvalException;
import org.joval.scap.sce.SceException;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of an SCAP 1.2 Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Datastream implements IDatastream, ILoggable {
    private LocLogger logger;
    private DataStream stream;
    private String streamId;
    private Dictionary dictionary;
    private Map<String, Object> hrefMap;
    private Map<String, Component> components;
    private Map<String, ExtendedComponent> extendedComponents;

    /**
     * Create a Datastream.
     */
    Datastream(DataStream stream, DataStreamCollection dsc) throws ScapException {
	this.stream = stream;
	streamId  = stream.getId();
	logger = JOVALMsg.getLogger();
	//
	// Index all the components available in the Datastream collection
	//
	components = new HashMap<String, Component>();
	for (Component component : dsc.getComponent()) {
	    components.put(component.getId(), component);
	}
	extendedComponents = new HashMap<String, ExtendedComponent>();
	for (ExtendedComponent component : dsc.getExtendedComponent()) {
	    extendedComponents.put(component.getId(), component);
	}

	//
	// Discover the dictionary
	//
	if (stream.isSetDictionaries() && stream.getDictionaries().getComponentRef().size() > 0) {
	    RefListType refs = stream.getDictionaries();
	    if (refs.getComponentRef().size() == 1) {
		String dictionaryId = refs.getComponentRef().get(0).getHref();
		if (dictionaryId.startsWith("#")) {
		    dictionaryId = dictionaryId.substring(1);
		}
		if (components.containsKey(dictionaryId)) {
		    dictionary = new Dictionary(components.get(dictionaryId).getCpeList());
		} else {
		    throw new ScapException("No dictionary: " + dictionaryId);
		}
	    } else if (refs.getComponentRef().size() > 1) {
		logger.warn("ERROR: Multiple dictionaries specified in stream " + streamId);
	    }
	} else {
	    logger.warn("WARNING: No dictionaries defined in stream " + streamId);
	}

	//
	// Index component mappings for this Datastream
	//
	Map<String, String> checks = new HashMap<String, String>();
	for (ComponentRef ref : stream.getChecks().getComponentRef()) {
	    String href = ref.getHref();
	    if (href.startsWith("#")) {
		href = href.substring(1);
	    }
	    checks.put(ref.getId(), href);
	}
	hrefMap = new HashMap<String, Object>();
	if (stream.isSetDictionaries()) {
	    for (ComponentRef ref : stream.getDictionaries().getComponentRef()) {
		if (ref.isSetCatalog()) {
		    addCatalog(ref.getCatalog(), checks);
		}
	    }
	}
	for (ComponentRef ref : stream.getChecklists().getComponentRef()) {
	    if (ref.isSetCatalog()) {
		addCatalog(ref.getCatalog(), checks);
	    }
	}
    }

    // Implement IDatastream

    public IDictionary getDictionary() {
	return dictionary;
    }

    /**
     * Return a collection of Benchmark component IDs, for the given stream ID.
     */
    public Collection<String> getBenchmarkIds() {
	Collection<String> benchmarkIds = new ArrayList<String>();
	for (ComponentRef ref : stream.getChecklists().getComponentRef()) {
	    String href = ref.getHref();
	    if (href.startsWith("#")) {
		href = href.substring(1);
	    }
	    benchmarkIds.add(href);
	}
	return benchmarkIds; 
    }

    public IBenchmark getBenchmark(String benchmarkId) throws NoSuchElementException, XccdfException {
	Component comp = getComponent(benchmarkId);
	if (comp.isSetBenchmark()) {
	    return new Benchmark(comp);
	}
	throw new NoSuchElementException(benchmarkId);
    }

    public Collection<String> getProfileIds(String benchmarkId) throws NoSuchElementException {
	Component comp = getComponent(benchmarkId);
	if (comp.isSetBenchmark()) {
	    Collection<String> result = new ArrayList<String>();
	    for (ProfileType profile : comp.getBenchmark().getProfile()) {
		result.add(profile.getProfileId());
	    }
	    return result;
	}
	throw new NoSuchElementException(benchmarkId);
    }

    public IView view(ITailoring tailoring, String profileId) throws NoSuchElementException, ScapException {
	String benchmarkId = tailoring.getBenchmarkId();
	if (components.containsKey(benchmarkId)) {
	    Component comp = components.get(benchmarkId);
	    if (comp.isSetBenchmark()) {
		return new View(this, new Benchmark(comp), tailoring.getProfile(profileId));
	    } else {
		throw new NoSuchElementException("Not a benchmark component: " + benchmarkId);
	    }
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    public IView view(String benchmarkId, String profileId) throws NoSuchElementException, ScapException {
	if (components.containsKey(benchmarkId)) {
	    Component comp = components.get(benchmarkId);
	    if (comp.isSetBenchmark()) {
		BenchmarkType bt = comp.getBenchmark();
		if (profileId == null) {
		    return new View(this, new Benchmark(comp), null);
		} else {
		    for (ProfileType profile : bt.getProfile()) {
			if (profile.getProfileId().equals(profileId)) {
			    return new View(this, new Benchmark(comp), profile);
			}
		    }
		    throw new NoSuchElementException(profileId);
		}
	    } else {
		throw new NoSuchElementException("Not a benchmark component: " + benchmarkId);
	    }
	} else {
	    throw new NoSuchElementException(benchmarkId);
	}
    }

    /**
     * Get the component corresponding to the specified href in this stream.
     */
    public Object resolve(String href) throws NoSuchElementException {
	if (hrefMap.containsKey(href)) {
	    return hrefMap.get(href);
	} else {
	    throw new NoSuchElementException(href);
	}
    }

    /**
     * Given a component href, get the SystemEnumeration type of the component.
     */
    public SystemEnumeration getSystem(String href) throws NoSuchElementException {
	Object obj = resolve(href);
	if (obj instanceof Component) {
	    Component component = (Component)obj;
	    if (component.isSetOvalDefinitions()) {
		return SystemEnumeration.OVAL;
	    } else if (component.isSetOcil()) {
		return SystemEnumeration.OCIL;
	    } else {
		return SystemEnumeration.UNSUPPORTED;
	    }
	} else if (obj instanceof ExtendedComponent) {
	    ExtendedComponent component = (ExtendedComponent)obj;
	    Object data = component.getAny();
	    if (data instanceof JAXBElement) {
		data = ((JAXBElement)data).getValue();
	    }
	    if (data instanceof ScriptDataType) {
		return SystemEnumeration.SCE;
	    } else {
		return SystemEnumeration.UNSUPPORTED;
	    }
	} else {
	    throw new NoSuchElementException(href);
	}
    }

    public IChecklist getOcil(String href) throws NoSuchElementException, OcilException {
	Object obj = resolve(href);
	if (obj instanceof Component) {
	    Component comp = (Component)obj;
	    if (comp.isSetOcil()) {
		return new Checklist(comp.getOcil());
	    } else {
		throw new OcilException("Not an OCIL component: " + href);
	    }
	} else {
	    throw new OcilException("Not a component: " + href);
	}
    }

    public IDefinitions getOval(String href) throws NoSuchElementException, OvalException {
	Object obj = resolve(href);
	if (obj instanceof Component) {
	    Component comp = (Component)obj;
	    if (comp.isSetOvalDefinitions()) {
		return new Definitions(comp.getOvalDefinitions());
	    } else {
		throw new OvalException("Not an OVAL component: " + href);
	    }
	} else {
	    throw new OvalException("Not a component: " + href);
	}
    }

    public ScriptDataType getSce(String href) throws NoSuchElementException, SceException {
	Object obj = resolve(href);
	if (obj instanceof ExtendedComponent) {
	    ExtendedComponent comp = (ExtendedComponent)obj;
	    Object data = comp.getAny();
	    if (data instanceof JAXBElement) {
		data = ((JAXBElement)data).getValue();
	    }
	    if (data instanceof ScriptDataType) {
		return (ScriptDataType)data;
	    } else {
		throw new SceException("Not an SCE component: " + href);
	    }
	} else {
	    throw new SceException("Not a component: " + href);
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

    private Component getComponent(String componentId) throws NoSuchElementException {
	if (components.containsKey(componentId)) {
	    return components.get(componentId);
	}
	throw new NoSuchElementException(componentId);
    }

    private ExtendedComponent getExtendedComponent(String componentId) throws NoSuchElementException {
	if (extendedComponents.containsKey(componentId)) {
	    return extendedComponents.get(componentId);
	}
	throw new NoSuchElementException(componentId);
    }

    /**
     * Add a catalog, which maps hrefs to component IDs.
     */
    private void addCatalog(Catalog catalog, Map<String, String> checks) {
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
			    if (hrefMap.containsKey(u.getName())) {
				logger.warn("ERROR: Duplicate URI name: " + u.getName());
			    } else {
				hrefMap.put(u.getName(), components.get(componentId));
			    }
			} else if (extendedComponents.containsKey(u.getName())) {
			    if (hrefMap.containsKey(u.getName())) {
				logger.warn("ERROR: Duplicate URI name: " + u.getName());
			    } else {
				hrefMap.put(u.getName(), extendedComponents.get(componentId));
			    }
			} else {
			    logger.warn("ERROR: No component found for id " + componentId);
			}
		    } else {
			logger.warn("ERROR: No check found for catalog URI " + uri);
		    }
		} else if (elt.isNil()) {
		    logger.warn("ERROR: Nil element");
		} else {
		    logger.warn("ERROR: Not a Uri: " + elt.getValue().getClass().getName());
		}
	    }
	}
    }
}
