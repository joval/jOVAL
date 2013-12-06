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
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.datastream.Component;
import scap.xccdf.GroupType;
import scap.xccdf.ItemType;
import scap.xccdf.ProfileType;
import scap.xccdf.RuleType;
import scap.xccdf.SelectableItemType;
import scap.xccdf.ValueType;
import scap.xccdf.XccdfBenchmark;

import org.joval.intf.scap.xccdf.IBenchmark;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;
import org.joval.xml.XSLTools;

/**
 * A representation of an XCCDF 1.2 benchmark.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Benchmark implements IBenchmark, ILoggable {
    private static final String LEGACY_NS = "http://checklists.nist.gov/xccdf/1.1";

    /**
     * Read a benchmark file.
     */
    public static final XccdfBenchmark getXccdfBenchmark(File f) throws XccdfException {
	try {
	    return getXccdfBenchmark(new FileInputStream(f));
	} catch (FileNotFoundException e) {
	    throw new XccdfException(e);
	}
    }

    /**
     * Read a benchmark from a stream.
     */
    public static final XccdfBenchmark getXccdfBenchmark(InputStream in) throws XccdfException {
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document doc = builder.parse(in);

	    StringBuffer prefix = new StringBuffer("xmlns");
	    if (doc.getDocumentElement().getPrefix() != null) {
		prefix.append(":").append(doc.getDocumentElement().getPrefix());
	    }
	    String ns = doc.getDocumentElement().getAttribute(prefix.toString());
	    if (LEGACY_NS.equals(ns)) {
		JOVALMsg.getLogger().info(JOVALMsg.STATUS_XCCDF_CONVERT);
		TransformerFactory xf = XSLTools.XSLVersion.V2.getFactory();
		InputStream xsl = Benchmark.class.getResourceAsStream("xccdf_convert_1.1.4_to_1.2.xsl");
		Transformer transformer = xf.newTransformer(new StreamSource(xsl));
		DOMResult result = new DOMResult();
		transformer.transform(new DOMSource(doc), result);
		return getXccdfBenchmark(new DOMSource(result.getNode()));
	    } else if (SystemEnumeration.XCCDF.namespace().equals(ns)) {
		return getXccdfBenchmark(new DOMSource(doc));
	    } else {
		throw new XccdfException(new IllegalArgumentException(ns));
	    }
	} catch (TransformerException e) {
	    throw new XccdfException(e);
	} catch (SAXException e) {
	    throw new XccdfException(e);
	} catch (IOException e) {
	    throw new XccdfException(e);
	} catch (javax.xml.parsers.FactoryConfigurationError e) {
	    throw new XccdfException(e.getMessage());
	} catch (ParserConfigurationException e) {
	    throw new XccdfException(e);
	}
    }

    /**
     * Read a benchmark from a JAXB source.
     */
    public static final XccdfBenchmark getXccdfBenchmark(Source source) throws XccdfException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.XCCDF.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof XccdfBenchmark) {
		return (XccdfBenchmark)rootObj;
	    } else {
		throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new XccdfException(e);
	}
    }

    private LocLogger logger;
    private XccdfBenchmark xb;
    private String href;
    private Map<String, ProfileType> profiles;
    private Map<String, ItemType> items;

    /**
     * Create a benchmark document from the JAXB datatype.
     */
    public Benchmark(String href, XccdfBenchmark xb) throws XccdfException {
	this();
	this.href = href;
	setBenchmark(xb);
    }

    /**
     * Create a benchmark document from a Datastream component.
     */
    public Benchmark(Component component) throws XccdfException {
	this();
	if (component.isSetBenchmark()) {
	    href = "#" + component.getId();
	    setBenchmark(component.getBenchmark());
	} else {
	    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_DATASTREAM_COMP_TYPE, component.getId(), "XCCDF"));
	}
    }

    // Implement IBenchmark

    public String getId() {
	return xb.getBenchmarkId();
    }

    public String getHref() {
	return href;
    }

    public ProfileType getProfile(String id) throws NoSuchElementException {
	if (profiles.containsKey(id)) {
	    return profiles.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public ItemType getItem(String id) throws NoSuchElementException {
	if (items.containsKey(id)) {
	    return items.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.XCCDF.createMarshaller();
	    out = new FileOutputStream(f);
	    marshaller.marshal(xb, out);
	} catch (JAXBException e) {
	    throw new IOException(e);
	} catch (javax.xml.stream.FactoryConfigurationError e) {
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

    public void writeTransform(Transformer transform, File output) {
	try {
	    transform.transform(getSource(), new StreamResult(output));
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (TransformerException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.XCCDF.getJAXBContext(), xb);
    }

    public XccdfBenchmark getRootObject() {
	return xb;
    }

    public XccdfBenchmark copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof XccdfBenchmark) {
	    return (XccdfBenchmark)rootObj;
	} else {
	    throw new XccdfException(JOVALMsg.getMessage(JOVALMsg.ERROR_XCCDF_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.XCCDF.getJAXBContext();
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    private Benchmark() throws XccdfException {
	logger = JOVALMsg.getLogger();
    }

    /**
     * Set xb and initialize the items map.
     */
    private void setBenchmark(XccdfBenchmark xb) {
	this.xb = xb;
	profiles = new HashMap<String, ProfileType>();
	for (ProfileType pt : xb.getProfile()) {
	    profiles.put(pt.getProfileId(), pt);
	}

	items = new HashMap<String, ItemType>();
	for (ValueType value : xb.getValue()) {
	    items.put(value.getId(), value);
	}
	for (SelectableItemType item : xb.getGroupOrRule()) {
	    addSelectableItem(item);
	}
    }

    /**
     * Recursively add the item and its children.
     */
    private void addSelectableItem(SelectableItemType item) {
	if (item instanceof RuleType) {
	    RuleType rule = (RuleType)item;
	    items.put(rule.getId(), rule);
	} else if (item instanceof GroupType) {
	    GroupType group = (GroupType)item;
	    items.put(group.getId(), group);
	    for (ValueType value : group.getValue()) {
		items.put(value.getId(), value);
	    }
	    for (SelectableItemType sit : group.getGroupOrRule()) {
		addSelectableItem(sit);
	    }
	}
    }
}
