// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.InputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility for working with XSI.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XSLTools {
    public static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";

    private static final DocumentBuilder BUILDER;
    static {
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    BUILDER = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Supported versions of XSL/XPATH. To support XSL 2, Saxon must be in the classpath of this class's classloader.
     */
    public enum XSLVersion {
	V1("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"),
	V2("net.sf.saxon.TransformerFactoryImpl");

	private TransformerFactory factory;
	private String className;

	private XSLVersion(String className) {
	    this.className = className;
	    factory = null;
	}

	public TransformerFactory getFactory() throws TransformerFactoryConfigurationError {
	    if (factory == null) {
		factory = TransformerFactory.newInstance(className, XSLTools.class.getClassLoader());
	    }
	    return factory;
	}
    }

    /**
     * Get a Transformer for the specified source.
     *
     * @throws IllegalArgumentException if the XSL version is not 1.X or 2.X
     * @throws NoSuchElementException if there is no xsl:stylesheet node in the document
     * @throws SAXException if there is a problem parsing the XML input
     * @throws IOException if there is an I/O problem encountered while parsing the input
     * @throws TransformerConfigurationException if there is a problem getting the factory or building the transformer
     */
    public static Transformer getTransformer(InputStream in) throws IllegalArgumentException, NoSuchElementException,
		IOException, SAXException, TransformerConfigurationException {
	Document doc = BUILDER.parse(in);
	NodeList nodes = doc.getChildNodes();
	for (int i=0; i < nodes.getLength(); i++) {
	    Node node = nodes.item(i);
	    switch(node.getNodeType()) {
	      case Node.ELEMENT_NODE:
		Element elt = (Element)node;
		if ("stylesheet".equals(elt.getLocalName()) && XSL_NS.equals(elt.getNamespaceURI())) {
		    String version = elt.getAttribute("version");
		    TransformerFactory xf = null;
		    if (version.startsWith("1.")) {
			xf = XSLVersion.V1.getFactory();
		    } else if (version.startsWith("2.")) {
			xf = XSLVersion.V2.getFactory();
		    } else {
			throw new IllegalArgumentException("XSL version: " + version);
		    }
		    return xf.newTransformer(new DOMSource(doc));
		}
		break;
	    }
	}
	throw new NoSuchElementException("xsl:stylesheet");
    }
}
