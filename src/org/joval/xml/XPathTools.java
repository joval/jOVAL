// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.util.Collection;
import java.util.Stack;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Useful methods for XPath evaluation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPathTools {
    public static Collection<String> typesafeEval(XPathExpression xpe, Document doc) {
	Stack<QName> types = new Stack<QName>();
	types.push(XPathConstants.BOOLEAN);
	types.push(XPathConstants.NODE);
	types.push(XPathConstants.NODESET);
	types.push(XPathConstants.NUMBER);
	types.push(XPathConstants.STRING);

	return typesafeEval(xpe, doc, types);
    }

    public static String getMessage(XPathExpressionException err) {
	return crawlMessage(err);
    }

    // Private

    private static Collection<String> typesafeEval(XPathExpression xpe, Document doc, Stack<QName> types) {
	Collection<String> list = new Vector<String>();
	if (types.empty()) {
	    return list;
	}
	try {
	    QName qn = types.pop();
	    Object o = xpe.evaluate(doc, qn);
	    if (o instanceof String) {
		list.add((String)o);
	    } else if (o instanceof NodeList) {
		NodeList nodes = (NodeList)o;
		int len = nodes.getLength();
		for (int i=0; i < len; i++) {
		    list.add(nodes.item(i).getNodeValue());
		}
	    } else if (o instanceof Double) {
		list.add(((Double)o).toString());
	    } else if (o instanceof Boolean) {
		list.add(((Boolean)o).toString());
	    }
	    return list;
	} catch (XPathExpressionException e) {
	    return typesafeEval(xpe, doc, types);
	}
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
}
