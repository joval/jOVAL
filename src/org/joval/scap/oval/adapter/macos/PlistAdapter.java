// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

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
import scap.oval.common.OperationEnumeration;
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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
	    baseInit(session);
	    classes.add(PlistObject.class);
	    classes.add(Plist510Object.class);
	} else {
	    notapplicable.add(PlistObject.class);
	    notapplicable.add(Plist510Object.class);
	}
	return classes;
    }

    // Implement IAdapter

    @Override
    public Collection<PlistItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Plist510Object pObj = getPlist510Object(obj);
	if (pObj.isSetFilepath()) {
	    //
	    // Delegate file discovery to the BaseFileAdapter
	    //
	    return super.getItems(obj, rc);
	} else {
	    //
	    // Create an item based on the appid and logged-in user account
	    //
	    Collection<PlistItem> items = new ArrayList<PlistItem>();
	    try {
		String prefs = session.getEnvironment().getenv("HOME") + "/Library/Preferences";
		String appid = (String)pObj.getAppId().getValue();
		OperationEnumeration op = pObj.getAppId().getOperation();
		switch(op) {
		  case EQUALS: {
		    IFile f = session.getFilesystem().getFile(prefs + appid + ".plist");
		    items.addAll(getItems(obj, createBaseItem(appid, f.getPath()), f, rc));
		    break;
		  }

		  case NOT_EQUAL: {
		    IFile dir = session.getFilesystem().getFile(prefs);
		    if (dir.isDirectory()) {
			for (IFile f : dir.listFiles()) {
			    String fname = dir.getName();
			    String id = fname.substring(0, fname.length() - 6);
			    if (!id.equals(appid)) {
				items.addAll(getItems(obj, createBaseItem(appid, f.getPath()), f, rc));
			    }
			}
		    }
		    break;
		  }

		  case PATTERN_MATCH: {
		    IFile dir = session.getFilesystem().getFile(prefs);
		    if (dir.isDirectory()) {
			Pattern p = Pattern.compile(appid);
			for (IFile f : dir.listFiles(Pattern.compile(".+\\.plist$"))) {
			    String fname = dir.getName();
			    String id = fname.substring(0, fname.length() - 6);
			    if (p.matcher(id).find()) {
				items.addAll(getItems(obj, createBaseItem(appid, f.getPath()), f, rc));
			    }
			}
		    }
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (IOException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    return items;
	}
    }

    // Protected

    /**
     * Given a PlistObject, return an equivalent Plist510Object. Given a Plist510Object, cast and return it.
     */
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
	    if (pObjIn.isSetFilepath()) {
		pObj.setFilepath(pObjIn.getFilepath());
	    }
	} else if (obj instanceof Plist510Object) {
	    pObj = (Plist510Object)obj;
	} else {
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
	}
	return pObj;
    }

    /**
     * Get the CFBundleIdentifier value from a parsed Plist object.
     */
    protected String findAppId(NSObject obj) throws CollectException {
	Collection<NSObject> values = findValues(obj, "CFBundleIdentifier");
	if (values.size() > 0) {
	    return getValue(values.iterator().next());
	} else {
	    return null;
	}
    }

    /**
     * Create a PlistItem containing the filepath and/or appid.
     */
    protected PlistItem createBaseItem(String appid, String filepath) {
	PlistItem item = Factories.sc.macos.createPlistItem();
	EntityItemStringType appIdType = Factories.sc.core.createEntityItemStringType();
	if (appid == null) {
	    appIdType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	} else {
	    appIdType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    appIdType.setValue(appid);
	}
	item.setAppId(appIdType);
	EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
	if (filepath == null) {
	    filepathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	} else {
	    filepathType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    filepathType.setValue(filepath);
	}
	item.setFilepath(filepathType);
	return item;
    }

    /**
     * Implementation of abstract method from BaseFileAdapter.
     */
    protected Class getItemClass() {
	return PlistItem.class;
    }

    /**
     * Implementation of abstract method from BaseFileAdapter.
     */
    protected Collection<PlistItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	Plist510Object pObj = getPlist510Object(obj);
	PlistItem baseItem = (PlistItem)base;

	if (pObj.getKey().getValue().getOperation() == OperationEnumeration.EQUALS) {
	    JAXBElement<EntityObjectStringType> key = pObj.getKey();
	    if (key.isNil()) {
		baseItem.setKey(Factories.sc.macos.createPlistItemKey(null));
	    } else {
		EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
		keyType.setValue(key.getValue().getValue());
		keyType.setDatatype(key.getValue().getDatatype());
		baseItem.setKey(Factories.sc.macos.createPlistItemKey(keyType));
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getKey().getValue().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	InputStream in = null;
	try {
	    in = f.getInputStream();
	    return getItems(getPlist510Object(obj), baseItem, PropertyListParser.parse(in));
	} catch (CollectException e) {
	    throw e;
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_PARSE, f.getPath(), e.getMessage());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
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

    /**
     * Return corresponding items from a parsed Plist object.
     */
    protected Collection<PlistItem> getItems(Plist510Object pObj, PlistItem base, NSObject obj) throws Exception {
	String key = null;
	if (!pObj.getKey().isNil()) {
	    key = (String)pObj.getKey().getValue().getValue();
	}

	//
	// Get all the value instances with the specified key
	//
	Map<Integer, PlistItem> instances = new HashMap<Integer, PlistItem>();
	int inst = 0;
	for (NSObject value : findValues(obj, key)) {
	    PlistItem item = Factories.sc.macos.createPlistItem();
	    item.setKey(base.getKey());
	    item.setAppId(base.getAppId());
	    item.setFilepath(base.getFilepath());
	    inst++; // start instance counting at 1

	    EntityItemIntType instanceType = Factories.sc.core.createEntityItemIntType();
	    instanceType.setValue(Integer.toString(inst));
	    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setInstance(instanceType);

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
	    instances.put(new Integer(inst), item);
	}

	//
	// Filter results according to the the instance operation.
	//
	Collection<PlistItem> items = new ArrayList<PlistItem>();
	if (pObj.isSetInstance()) {
	    Integer instance = new Integer((String)pObj.getInstance().getValue());
	    OperationEnumeration op = pObj.getInstance().getOperation();
	    for (Map.Entry<Integer, PlistItem> entry : instances.entrySet()) {
		switch(op) {
		  case EQUALS:
		    if (entry.getKey().compareTo(instance) == 0) {
			items.add(entry.getValue());
		    }
		    break;

		  case NOT_EQUAL:
		    if (entry.getKey().compareTo(instance) != 0) {
			items.add(entry.getValue());
		    }
		    break;

		  case GREATER_THAN:
		    if (entry.getKey().compareTo(instance) > 0) {
			items.add(entry.getValue());
		    }
		    break;

		  case GREATER_THAN_OR_EQUAL:
		    if (entry.getKey().compareTo(instance) >= 0) {
			items.add(entry.getValue());
		    }
		    break;

		  case LESS_THAN:
		    if (entry.getKey().compareTo(instance) < 0) {
			items.add(entry.getValue());
		    }
		    break;

		  case LESS_THAN_OR_EQUAL:
		    if (entry.getKey().compareTo(instance) <= 0) {
			items.add(entry.getValue());
		    }
		    break;

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	} else {
	    items.addAll(instances.values());
	}
	return items;
    }

    // Private

    /**
     * Recursively search an object for values with the given key.
     */
    private Collection<NSObject> findValues(NSObject obj, String key) {
	Collection<NSObject> values = new ArrayList<NSObject>();

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
