// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.system.IComputerSystem;
import jsaf.intf.system.ISession;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.independent.XmlfilecontentObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.independent.XmlfilecontentItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IComputerSystem) {
	    try {
		baseInit((IComputerSystem)session);
		classes.add(XmlfilecontentObject.class);
	    } catch (UnsupportedOperationException e) {
		// doesn't support ISession.getFilesystem()
		notapplicable.add(XmlfilecontentObject.class);
	    }
	} else {
	    notapplicable.add(XmlfilecontentObject.class);
	}
	return classes;
    }

    // Implement IBatch

    @Override
    public Collection<IResult> exec() {
	Map<IRequest, IResult> resultMap = new HashMap<IRequest, IResult>();
	Map<String, Collection<IRequest>> requestMap = new HashMap<String, Collection<IRequest>>();
	try {
	    //
	    // Organize requests and files by path.
	    //
	    String[] paths = queuePaths();
	    IFile[] files = session.getFilesystem().getFiles(paths);
	    Map<String, IFile> fileMap = new HashMap<String, IFile>();
	    for (int i=0; i < paths.length; i++) {
		String path = paths[i];
		fileMap.put(path, files[i]);
		IRequest request = queue.get(i);
		if (requestMap.containsKey(path)) {
		    requestMap.get(path).add(request);
		} else {
		    ArrayList<IRequest> rl = new ArrayList<IRequest>();
		    rl.add(request);
		    requestMap.put(path, rl);
		}
	    }

	    //
	    // Iterate across distinct paths, so each file actually is retrieved only once.
	    //
	    for (Map.Entry<String, Collection<IRequest>> entry : requestMap.entrySet()) {
		IFile f = fileMap.get(entry.getKey());
		if (f != null && f.isFile() && f.exists()) {
		    Document doc = DOMTools.parse(f.getInputStream());
		    for (IRequest request : entry.getValue()) {
			XmlfilecontentObject xObj = (XmlfilecontentObject)request.getObject();
			XmlfilecontentItem item = (XmlfilecontentItem)getBaseItem(xObj, f);

			EntityItemStringType xpathType = Factories.sc.core.createEntityItemStringType();
			String expression = (String)xObj.getXpath().getValue();
			xpathType.setValue(expression);
			item.setXpath(xpathType);

			IRequestContext rc = request.getContext();
			try {
			    XPathExpression expr = XPathTools.compile(expression);
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
			    resultMap.put(request, new Batch.Result(Arrays.asList(item), rc));
			} catch (Exception e) {
			    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
			}
		    }
		} else {
		    @SuppressWarnings("unchecked")
		    Collection<XmlfilecontentItem> empty = (Collection<XmlfilecontentItem>)Collections.EMPTY_LIST;
		    for (IRequest request : entry.getValue()) {
			resultMap.put(request, new Batch.Result(empty, request.getContext()));
		    }
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    for (IRequest request : queue) {
		IRequestContext rc = request.getContext();
		resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
	    }
	}
	queue = null;
	return resultMap.values();
    }

    // Protected

    protected Class getItemClass() {
	return XmlfilecontentItem.class;
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<XmlfilecontentItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	Collection<XmlfilecontentItem> items = new ArrayList<XmlfilecontentItem>();
	XmlfilecontentObject xObj = (XmlfilecontentObject)obj;
	for (IFile f : files) {
	    try {
		XmlfilecontentItem item = (XmlfilecontentItem)getBaseItem(obj, f);
		if (item != null) {
		    EntityItemStringType xpathType = Factories.sc.core.createEntityItemStringType();
		    String expression = (String)xObj.getXpath().getValue();
		    xpathType.setValue(expression);
		    item.setXpath(xpathType);

		    InputStream in = null;
		    try {
			XPathExpression expr = XPathTools.compile(expression);
			in = f.getInputStream();
			Document doc = DOMTools.parse(in);
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
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_XML_XPATH, expression, XPathTools.getMessage(e));
			throw new CollectException(msg, FlagEnumeration.ERROR);
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
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }
}
