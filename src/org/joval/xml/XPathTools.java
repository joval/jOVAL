// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jsaf.util.StringTools;

/**
 * Useful methods for XPath evaluation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPathTools {
    /**
     * The QName types, in evaluation preference order.
     */
    private static final QName[] TYPES = {XPathConstants.NODESET,
					  XPathConstants.NODE,
					  XPathConstants.STRING,
					  XPathConstants.NUMBER,
					  XPathConstants.BOOLEAN};

    private static DocumentBuilder builder;
    private static XPath xpath;
    static {
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    builder = factory.newDocumentBuilder();
	    xpath = XPathFactory.newInstance().newXPath();
	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Convenience method, for parsing an InputStream into a DOM Document.
     */
    public static synchronized Document parse(InputStream in) throws SAXException, IOException {
	return builder.parse(in);
    }

    /**
     * Convenience method, for compiling an XPath Expression.
     */
    public static synchronized XPathExpression compile(String s) throws XPathExpressionException {
	return xpath.compile(s);
    }

    /**
     * Returns the String result of the XPath query. This may be XML in String form, for instance.
     */
    public static List<String> typesafeEval(XPathExpression xpe, Document doc) throws TransformerException {
	for (QName qn : TYPES) {
	    try {
		return eval(xpe, doc, qn);
	    } catch (XPathExpressionException e) {
	    }
	}
	return new Vector<String>();
    }

    /**
     * Extract an intelligible error message from an XPathExpressionException.
     */
    public static String getMessage(XPathExpressionException err) {
	return crawlMessage(err);
    }

    // Private

    private static List<String> eval(XPathExpression xpe, Document doc, QName qn)
		throws TransformerException, XPathExpressionException {

	List<String> list = new Vector<String>();
	Object o = xpe.evaluate(doc, qn);
	if (o instanceof NodeList) {
	    NodeList nodes = (NodeList)o;
	    int len = nodes.getLength();
	    for (int i=0; i < len; i++) {
		DOMSource src = new DOMSource(nodes.item(i));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamResult res = new StreamResult(out);
		getTransformer().transform(src, res);
		list.add(new String(out.toByteArray(), StringTools.UTF8));
	    }
	} else if (o instanceof Double) {
	    list.add(((Double)o).toString());
	} else if (o instanceof Boolean) {
	    list.add(((Boolean)o).toString());
	} else if (o instanceof String) {
	    list.add((String)o);
	} else {
	    list.add(o.toString());
	}
	return list;
    }

    private static String crawlMessage(Throwable t) {
	if (t == null) {
	    return "null";
	} else {
	    String s = t.getMessage();
	    if (s == null) {
		return crawlMessage(t.getCause());
	    } else {
		return s;
	    }
	}
    }

    private static Transformer transformer = null;

    private static Transformer getTransformer() throws TransformerException {
	if (transformer == null) {
	    transformer = TransformerFactory.newInstance().newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	}
	return transformer;
    }
}
