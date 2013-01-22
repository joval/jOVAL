// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Stack;
import java.util.Vector;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;

import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDate;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSSet;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.macos.PlistObject;
import scap.oval.definitions.macos.Plist510Object;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.macos.PlistItem;
import scap.oval.systemcharacteristics.macos.EntityItemPlistTypeType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Retrieves items for Plist objects. See the following URL for more information (as the specification is quite vague):
 * http://making-security-measurable.1364806.n2.nabble.com/Is-There-a-Need-for-a-macos-plist-test-td5367977.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PlistAdapter extends BaseFileAdapter<PlistItem> {
    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    switch(((IUnixSession)session).getFlavor()) {
	      case MACOSX:
		baseInit(session);
		classes.add(PlistObject.class);
		classes.add(Plist510Object.class);
		break;
	    }
	}
	return classes;
    }

    // Implement IAdapter

    @Override
    public Collection<PlistItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	boolean fileBased = false;
	if (obj instanceof PlistObject) {
	    PlistObject pObj = (PlistObject)obj;
	    fileBased = pObj.isSetFilepath();
	} else {
	    Plist510Object pObj = (Plist510Object)obj;
	    fileBased = pObj.isSetFilepath();
	}
	if (fileBased) {
	    //
	    // Delegate to the BaseFileAdapter
	    //
	    return super.getItems(obj, rc);
	} else {
	    //
	    // Create an item based on the appid and current user account
	    //
	    String appid = getAppId(obj);
	    if (appid == null) {
		String message = JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_PLIST_OBJECT, obj.getId());
		throw new CollectException(message, FlagEnumeration.ERROR);
	    } else {
		PlistItem base = Factories.sc.macos.createPlistItem();
		EntityItemStringType appId = Factories.sc.core.createEntityItemStringType();
		appId.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		appId.setValue(appid);
		base.setAppId(appId);

		StringBuffer sb = new StringBuffer(session.getEnvironment().getenv("HOME"));
		String path = sb.append("/Library/Preferences/").append(appid).append(".plist").toString();
		EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
		filepath.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		filepath.setValue(path);
		base.setFilepath(filepath);

		try {
		    IFile f = session.getFilesystem().getFile(path);
		    return getItems(obj, base, f, rc);
		} catch (IOException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(e.getMessage());
		    rc.addMessage(msg);
		}
	    }
	}
	@SuppressWarnings("unchecked")
	Collection<PlistItem> empty = (Collection<PlistItem>)Collections.EMPTY_LIST;
	return empty;
    }

    // Protected

    protected Class getItemClass() {
	return PlistItem.class;
    }

    /**
     * Entry point for the BaseFileAdapter super-class. Parse the plist specified by the Object, and decorate the Item.
     */
    protected Collection<PlistItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	PlistItem baseItem = null;
	if (base instanceof PlistItem) {
	    baseItem = (PlistItem)base;
	} else {
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
	}

	if (baseItem != null && f.isFile()) {
	    InputStream in = null;
	    try {
		in = f.getInputStream();
		return getItems(getPlist510Object(obj), baseItem.getFilepath(), PropertyListParser.parse(in));
	    } catch (Exception e) {
		if (e instanceof CollectException) {
		    throw (CollectException)e;
		} else if (e instanceof IOException) {
		    throw (IOException)e;
		} else {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_PARSE, f.getPath(), e.getMessage()));
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
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

	@SuppressWarnings("unchecked")
	Collection<PlistItem> empty = (Collection<PlistItem>)Collections.EMPTY_LIST;
	return empty;
    }

    protected Plist510Object getPlist510Object(ObjectType obj) throws CollectException {
	Plist510Object pObj = null;
	if (obj instanceof PlistObject) {
	    pObj = Factories.definitions.macos.createPlist510Object();
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
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
	}
	return pObj;
    }

    protected Collection<PlistItem> getItems(Plist510Object pObj, EntityItemStringType filepath, NSObject obj)
		throws Exception {

	String appId = null;
	if (pObj.isSetAppId()) {
	    appId = (String)pObj.getAppId().getValue();
	} else {
	    appId = findAppId(obj);
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
		    throw new CollectException(e, FlagEnumeration.ERROR);
		}
	    }
	}

	Collection<PlistItem> items = new Vector<PlistItem>();

	int inst = 0;
	for (NSObject value : findValues(obj, key)) {
	    PlistItem item = Factories.sc.macos.createPlistItem();
	    item.setFilepath(filepath);
	    inst++; // start instance counting at 1

	    if (instance != null) {
		if (instance.intValue() != inst) {
		    continue;
		}
	    }
	    EntityItemIntType instanceType = Factories.sc.core.createEntityItemIntType();
	    instanceType.setValue(Integer.toString(inst));
	    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setInstance(instanceType);

	    if (appId != null) {
		EntityItemStringType appIdType = Factories.sc.core.createEntityItemStringType();
		appIdType.setValue(appId);
		item.setAppId(appIdType);
	    }

	    if (key != null) {
		EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
		keyType.setValue(key);
		item.setKey(Factories.sc.macos.createPlistItemKey(keyType));
	    }

	    EntityItemPlistTypeType typeType = Factories.sc.macos.createEntityItemPlistTypeType();
	    if (value instanceof NSArray) {
		typeType.setValue("CFArray");
		NSObject[] array = ((NSArray)value).getArray();
		for (int i=0; i < array.length; i++) {
		    EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
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
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, value.getClass().getName());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
    
		EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
		val.setValue(getValue(value));
		item.getValue().add(val);
	    }
	    item.setType(typeType);
	    items.add(item);
	}

	return items;
    }

    // Private

    /**
     * Get the app ID from the object.
     */
    private String getAppId(ObjectType obj) {
	EntityObjectStringType appIdType = null;
	if (obj instanceof PlistObject) {
	    appIdType = ((PlistObject)obj).getAppId();
	} else if (obj instanceof Plist510Object) {
	    appIdType = ((Plist510Object)obj).getAppId();
	}
	return (String)appIdType.getValue();
    }

    private String findAppId(NSObject obj) throws CollectException {
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

    private String getValue(NSObject obj) throws CollectException {
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
	String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, obj.getClass().getName());
	throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
    }
}
