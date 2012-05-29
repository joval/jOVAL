// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.netconf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.netconf.ConfigObject;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.netconf.ConfigItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.net.INetconf;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.xml.XPathTools;

/**
 * Collects netconf:config_items from supported devices.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ConfigAdapter implements IAdapter {
    private INetconf session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof INetconf) {
	    try {
		this.session = (INetconf)session;
		classes.add(ConfigObject.class);
	    } catch (RuntimeException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	ConfigObject cObj = (ConfigObject)obj;
	String expression = (String)cObj.getXpath().getValue();
	XPathExpression xpe = null;
	try {
	    xpe = XPathTools.compile(expression);
	} catch (XPathExpressionException e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_XML_XPATH, expression, XPathTools.getMessage(e));
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	Document config = null;
	try {
	    config = session.getConfig();
	} catch (Exception e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_NETCONF_GETCONFIG, e.getMessage());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	ConfigItem item = Factories.sc.netconf.createConfigItem();
	EntityItemStringType xpathType = Factories.sc.core.createEntityItemStringType();
	xpathType.setValue(expression);
	item.setXpath(xpathType);

	try {
	    List<String> values = XPathTools.typesafeEval(xpe, config);
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
	} catch (TransformerException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_XML_TRANSFORM, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	Collection<ConfigItem> items = new Vector<ConfigItem>();
	items.add(item);
	return items;
    }
}
