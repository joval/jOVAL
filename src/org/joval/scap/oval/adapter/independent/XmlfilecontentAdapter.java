// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
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
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.xml.XPathTools;

/**
 * Evaluates Xmlfilecontent OVAL tests.
 *
 * DAS: Specify a maximum file size supported?
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XmlfilecontentAdapter extends BaseFileAdapter<XmlfilecontentItem> {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
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
	XmlfilecontentObject xObj = (XmlfilecontentObject)obj;
	XmlfilecontentItem baseItem = (XmlfilecontentItem)base;
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
		XPathExpression expr = XPathTools.compile(expression);
		in = f.getInputStream();
		Document doc = XPathTools.parse(in);
		List<String> values = XPathTools.typesafeEval(expr, doc);
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
		items.add(item);
	    } catch (XPathExpressionException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_XPATH, expression, XPathTools.getMessage(e)));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (SAXException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_PARSE, f.getPath(), e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (TransformerException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_TRANSFORM, e.getMessage()));
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
}
