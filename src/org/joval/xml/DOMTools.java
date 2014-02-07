// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.joval.intf.xml.ITransformable;
import org.joval.util.JOVALMsg;

/**
 * Utility for working with XML DOM.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DOMTools {
    private static final DocumentBuilder builder;
    static {
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(false);
	    builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Convenience method, for parsing an InputStream into a DOM Document. Note that the DocumentBuilder for this
     * class is NOT namespace-aware.
     */
    public static synchronized Document parse(InputStream in) throws SAXException, IOException {
	return builder.parse(in);
    }

    /**
     * Transform a JAXB object into a W3C Node.
     */
    public static Document toDocument(ITransformable<?> source) throws Exception {
	TransformerFactory xf = XSLTools.XSLVersion.V1.getFactory();
	Transformer transformer = xf.newTransformer();
	DOMResult result = new DOMResult();

	//
	// There's some bug in the Java transformer that makes it unsafe when thrashed statically by multiple
	// threads (even though that should work just fine) -- a ConcurrentModificationException is generated
	// internally, leading to a TransformerException.
	//
	// So, if this happens, we just retry after waiting a millisecond. But if it keeps happening after a
	// thousand attempts, we give up.
	//
	TransformerException te = null;
	for(int i=0; i < 1000; i++) {
	    try {
		transformer.transform(source.getSource(), result);
		return (Document)result.getNode();
	    } catch (TransformerException e) {
		te = e;
		if (e.getCause() instanceof java.util.ConcurrentModificationException) {
		    try {
			Thread.sleep(1);
		    } catch (InterruptedException ie) {
		    }
		} else {
		    throw te;
		}
	    }
	}
	throw te;
    }

    /**
     * Get the XML namespace of the specified ITransformable's root node.
     */
    public static String getNamespace(ITransformable<?> source) {
	try {
	    JAXBIntrospector ji = source.getJAXBContext().createJAXBIntrospector();
	    return ji.getElementName(source.getRootObject()).getNamespaceURI();
	} catch (JAXBException e) {
	    return null;
	}
    }

    /**
     * Convert the specified XML node into a pretty String (very useful for debugging).
     */
    public static String toString(Node node) throws Exception {
	TransformerFactory xf = XSLTools.XSLVersion.V1.getFactory();
	Transformer transformer = xf.newTransformer();
	StringWriter buff = new StringWriter();
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	transformer.transform(new DOMSource(node), new StreamResult(buff));
	return buff.toString();
    }
}
