// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Stack;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.XmlfilecontentObject;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.independent.XmlfilecontentItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * Evaluates Xmlfilecontent OVAL tests.
 *
 * DAS: Specify a maximum file size supported?
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XmlfilecontentAdapter extends BaseFileAdapter<XmlfilecontentItem> {
    private DocumentBuilder builder;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		builder = factory.newDocumentBuilder();
	    } catch (ParserConfigurationException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    classes.add(XmlfilecontentObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return XmlfilecontentItem.class;
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<XmlfilecontentItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException {

	Collection<XmlfilecontentItem> items = new Vector<XmlfilecontentItem>();
	XmlfilecontentObject xObj = null;
	if (obj instanceof XmlfilecontentObject) {
	    xObj = (XmlfilecontentObject)obj;
	}
	XmlfilecontentItem baseItem = null;
	if (base instanceof XmlfilecontentItem) {
	    baseItem = (XmlfilecontentItem)base;
	}
	if (baseItem != null && xObj != null && f.isFile()) {
	    XmlfilecontentItem item = Factories.sc.independent.createXmlfilecontentItem();
	    item.setPath(baseItem.getPath());
	    item.setFilename(baseItem.getFilename());
	    item.setFilepath(baseItem.getFilepath());
	    EntityItemStringType xpathType = Factories.sc.core.createEntityItemStringType();
	    String expression = (String)xObj.getXpath().getValue();
	    xpathType.setValue(expression);
	    item.setXpath(xpathType);

	    InputStream in = null;
	    try {
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(expression);
		in = f.getInputStream();
		Document doc = builder.parse(in);

		for (String value : typesafeEval(expr, doc)) {
		    EntityItemAnySimpleType valueOf = Factories.sc.core.createEntityItemAnySimpleType();
		    valueOf.setValue(value);
		    item.getValueOf().add(valueOf);
		}

		items.add(item);
	    } catch (XPathExpressionException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_XPATH, expression, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (SAXException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_PARSE, f.getPath(), e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			session.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
		    }
		}
	    }
	}
	return items;
    }

    private Collection<String> typesafeEval(XPathExpression expr, Document doc) {
	Stack<QName> types = new Stack<QName>();
	types.push(XPathConstants.BOOLEAN);
	types.push(XPathConstants.NODE);
	types.push(XPathConstants.NODESET);
	types.push(XPathConstants.NUMBER);
	types.push(XPathConstants.STRING);

	return typesafeEval(expr, doc, types);
    }

    private Collection<String> typesafeEval(XPathExpression exp, Document doc, Stack<QName> types) {
	Collection<String> list = new Vector<String>();
	if (types.empty()) {
	    return list;
	}
	try {
	    QName qn = types.pop();
	    Object o = exp.evaluate(doc, qn);
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
	    return typesafeEval(exp, doc, types);
	}
    }
}
