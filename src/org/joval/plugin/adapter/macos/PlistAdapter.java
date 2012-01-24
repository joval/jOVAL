// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.macos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Stack;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDate;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSSet;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.macos.PlistObject;
import oval.schemas.definitions.macos.Plist510Object;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.macos.PlistItem;
import oval.schemas.systemcharacteristics.macos.EntityItemPlistTypeType;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.ISession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.plugin.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Retrieves items for Plist objects. See the following URL for more information (as the specification is quite vague):
 * http://making-security-measurable.1364806.n2.nabble.com/Is-There-a-Need-for-a-macos-plist-test-td5367977.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PlistAdapter extends BaseFileAdapter {
    public PlistAdapter(ISession session) {
	super(session);
    }

    // Implement IAdapter

    private static Class[] objectClasses = {PlistObject.class, Plist510Object.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return filename;
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.macos.createPlistItem();
    }

    /**
     * Parse the plist specified by the Object, and decorate the Item.
     */
    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, NotCollectableException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	PlistItem baseItem = (PlistItem)base;

	ObjectType obj = rc.getObject();
	Plist510Object pObj = null;
	if (obj instanceof PlistObject) {
	    pObj = JOVALSystem.factories.definitions.macos.createPlist510Object();
	    PlistObject pObjIn = (PlistObject)obj;
	    if (pObjIn.isSetAppId()) {
		pObj.setAppId(pObjIn.getAppId());
	    }
	    if (pObjIn.isSetKey()) {
		pObj.setKey(pObjIn.getKey());
	    }
	} else if (obj instanceof Plist510Object) {
	    pObj = (Plist510Object)obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName()));
	}

	if (baseItem != null && pObj != null && f.isFile()) {
	    InputStream in = null;
	    try {
		in = f.getInputStream();
		NSObject nso = PropertyListParser.parse(in);

		String appId = null;
		if (pObj.isSetAppId()) {
		    appId = (String)pObj.getAppId().getValue();
		} else {
		    appId = findAppId(nso);
		}
    
		String key = null;
		if (pObj.isSetKey()) {
		    key = (String)pObj.getKey().getValue().getValue();
		}
    
		Integer instance = null;
		if (pObj.isSetInstance()) {
		    String s = (String)pObj.getInstance().getValue();
    
		    if (s != null && s.length() > 0) {
			try {
			    instance = new Integer(s);
			} catch (NumberFormatException e) {
			    throw new OvalException(e);
			}
		    }
		}

		for (PlistItem item : getItems(appId, key, instance, nso)) {
		    item.setFilepath(baseItem.getFilepath());
		    items.add(JOVALSystem.factories.sc.macos.createPlistItem(item));
		}
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_PARSE, f.getLocalName(), e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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

    // Private

    private Collection<PlistItem> getItems(String appId, String key, Integer instance, NSObject obj) throws Exception {
	Collection<PlistItem> items = new Vector<PlistItem>();

	int inst = 0;
	for (NSObject value : findValues(obj, key)) {
	    PlistItem item = (PlistItem)createFileItem();
	    inst++; // start instance counting at 1

	    if (instance != null) {
		if (instance.intValue() != inst) {
		    continue;
		}
	    }
	    EntityItemIntType instanceType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    instanceType.setValue(Integer.toString(inst));
	    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setInstance(instanceType);

	    if (appId != null) {
		EntityItemStringType appIdType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		appIdType.setValue(appId);
		item.setAppId(appIdType);
	    }

	    if (key != null) {
		EntityItemStringType keyType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		keyType.setValue(key);
		item.setKey(JOVALSystem.factories.sc.macos.createPlistItemKey(keyType));
	    }

	    EntityItemPlistTypeType typeType = JOVALSystem.factories.sc.macos.createEntityItemPlistTypeType();
	    if (value instanceof NSArray) {
		typeType.setValue("CFArray");
		NSObject[] array = ((NSArray)value).getArray();
		for (int i=0; i < array.length; i++) {
		    EntityItemAnySimpleType val = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		    val.setValue(getValue(array[i]));
		    item.getValue().add(val);
		}
	    } else {
		if (value instanceof NSData) {
		    typeType.setValue("CFData");
		} else if (value instanceof NSDate) {
		    typeType.setValue("CFDate");
		} else if (value instanceof NSNumber) {
		    NSNumber num = (NSNumber)value;
		    switch(num.type()) {
		      case NSNumber.BOOLEAN:
			typeType.setValue("CFBoolean");
			break;
		      case NSNumber.INTEGER:
			typeType.setValue("CFNumber");
			break;
		      case NSNumber.REAL:
			typeType.setValue("CFNumber");
			break;
		    }
		} else if (value instanceof NSString) {
		    typeType.setValue("CFString");
		} else {
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, value.getClass().getName());
		    throw new NotCollectableException(msg);
		}
    
		EntityItemAnySimpleType val = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		val.setValue(getValue(value));
		item.getValue().add(val);
	    }
	    item.setType(typeType);
	    items.add(item);
	}

	return items;
    }

    private String findAppId(NSObject obj) throws NotCollectableException {
	Collection<NSObject> values = findValues(obj, "CFBundleIdentifier");
	if (values.size() > 0) {
	    return getValue(values.iterator().next());
	} else {
	    return null;
	}
    }

    /**
     * Recursively search an object for values with the given key.
     */
    private Collection<NSObject> findValues(NSObject obj, String key) {
	Collection<NSObject> values = new Vector<NSObject>();

	if (key == null) {
	    values.add(obj);
	} else if (obj instanceof NSDictionary) {
	    NSDictionary dictionary = (NSDictionary)obj;
	    String[] keys = dictionary.allKeys();
	    for (int i=0; i < keys.length; i++) {
		if (keys[i].equals(key)) {
		    values.add(dictionary.objectForKey(key));
		} else {
		    values.addAll(findValues(dictionary.objectForKey(keys[i]), key));
		}
	    }
	} else if (obj instanceof NSArray) {
	    NSObject[] array = ((NSArray)obj).getArray();
	    for (int i=0; i < array.length; i++) {
		values.addAll(findValues(array[i], key));
	    }
	}

	return values;
    }

    private String getValue(NSObject obj) throws NotCollectableException {
	if (obj instanceof NSData) {
	    return ((NSData)obj).getBase64EncodedData();
	} else if (obj instanceof NSDate) {
	    return ((NSDate)obj).toString();
	} else if (obj instanceof NSNumber) {
	    NSNumber num = (NSNumber)obj;
	    switch(num.type()) {
	      case NSNumber.BOOLEAN:
	        return Boolean.valueOf(((NSNumber)obj).boolValue()).toString();
	      case NSNumber.INTEGER:
	        return Integer.valueOf(((NSNumber)obj).intValue()).toString();
	      case NSNumber.REAL:
	        return Float.valueOf(((NSNumber)obj).floatValue()).toString();
	    }
	} else if (obj instanceof NSString) {
	    return ((NSString)obj).toString();
	}
	String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, obj.getClass().getName());
	throw new NotCollectableException(msg);
    }
}
