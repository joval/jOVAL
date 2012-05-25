// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.junos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.junos.XmlLineObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.junos.XmlLineItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;
import org.joval.xml.XPathTools;

/**
 * Provides Juniper JunOS XML line OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XmlLineAdapter implements IAdapter {
    private static final String XML_DOC_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private DocumentBuilder builder;
    private XPath xpath;
    private IJunosSession session;
    private long readTimeout = 0;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IJunosSession) {
	    try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		builder = factory.newDocumentBuilder();

		xpath = XPathFactory.newInstance().newXPath();
		readTimeout = session.getProperties().getLongProperty(IJunosSession.PROP_READ_TIMEOUT);
		this.session = (IJunosSession)session;
		classes.add(XmlLineObject.class);
	    } catch (ParserConfigurationException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return classes;
    }

    public Collection<XmlLineItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	XmlLineObject xObj = (XmlLineObject)obj;
	String expression = (String)xObj.getXpath().getValue();
	XPathExpression xpe = null;
	try {
	    xpe = xpath.compile(expression);
	} catch (XPathExpressionException e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_XML_XPATH, expression, XPathTools.getMessage(e));
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	OperationEnumeration op = xObj.getShowSubcommand().getOperation();
	if (op != OperationEnumeration.EQUALS) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	XmlLineItem item = Factories.sc.junos.createXmlLineItem();
	EntityItemStringType xpathType = Factories.sc.core.createEntityItemStringType();
	xpathType.setValue(expression);
	item.setXpath(xpathType);
	String subcommand = (String)xObj.getShowSubcommand().getValue();
	EntityItemStringType showSubcommandType = Factories.sc.core.createEntityItemStringType();
	showSubcommandType.setValue(subcommand);
	item.setShowSubcommand(showSubcommandType);
	try {
	    List<String> values = XPathTools.typesafeEval(xpe, getDocument(subcommand));
	    if (values.size() == 0) {
		EntityItemAnySimpleType valueOf = Factories.sc.core.createEntityItemAnySimpleType();
		valueOf.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		item.getValueOf().add(valueOf);
	    } else {
		for (String value : values) {
		    EntityItemAnySimpleType valueOf = Factories.sc.core.createEntityItemAnySimpleType();
		    valueOf.setValue(value);
		    item.getValueOf().add(valueOf);
		}
	    }
	} catch (SAXException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_PARSE, subcommand, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_JUNOS_SHOW, subcommand, e.getMessage());
	    msg.setValue(s);
	    rc.addMessage(msg);
	    session.getLogger().warn(s);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	Collection<XmlLineItem> items = new Vector<XmlLineItem>();
	items.add(item);
	return items;
    }

    // Private

    private Document getDocument(String subcommand) throws Exception {
	if (!subcommand.toLowerCase().startsWith("show ")) {
	    subcommand = new StringBuffer("show ").append(subcommand).toString();
	}
	if (!subcommand.toLowerCase().endsWith(" | display xml")) {
	    subcommand = new StringBuffer(subcommand).append(" | display xml").toString();
	}

	ByteArrayOutputStream out = new ByteArrayOutputStream();
//	out.write(XML_DOC_HEADER.getBytes());
	SafeCLI.ExecData data = SafeCLI.execData(subcommand, null, session, readTimeout);
	out.write(data.getData());
System.out.println(new String(out.toByteArray(), StringTools.UTF8));
	return builder.parse(new ByteArrayInputStream(out.toByteArray()));
    }
}
