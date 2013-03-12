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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.joval.util.JOVALSystem;

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
	try {
	    baseInit(session);
	    classes.add(PlistObject.class);
	    classes.add(Plist510Object.class);
	} catch (UnsupportedOperationException e) {
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
	} else if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
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
	} else {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_APPID), FlagEnumeration.NOT_APPLICABLE);
	}
    }

    // Protected

    /**
     * Given a PlistObject, return an equivalent Plist510Object. Given a Plist510Object, cast and return it.
     */
    protected Plist510Object getPlist510Object(ObjectType obj) throws CollectException {
	if (obj instanceof PlistObject) {
	    PlistObject pObjIn = (PlistObject)obj;
	    Plist510Object pObj = Factories.definitions.macos.createPlist510Object();
	    if (pObjIn.isSetAppId()) {
		pObj.setAppId(pObjIn.getAppId());
	    }
	    if (pObjIn.isSetKey()) {
		pObj.setKey(pObjIn.getKey());
	    }
	    if (pObjIn.isSetFilepath()) {
		pObj.setFilepath(pObjIn.getFilepath());
	    }
	    return pObj;
	} else if (obj instanceof Plist510Object) {
	    return (Plist510Object)obj;
	} else {
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
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
	    if (JOVALSystem.isNil(key)) {
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
	//
	// Create a map of NSObjects corresponding to the object key spec.
	//
	Map<JAXBElement<EntityItemStringType>, List<NSObject>> values =
		new HashMap<JAXBElement<EntityItemStringType>, List<NSObject>>();
	if (JOVALSystem.isNil(pObj.getKey())) {
	    values.put(Factories.sc.macos.createPlistItemKey(null), findValues(obj, null));
	} else {
	    Collection<String> keys = new ArrayList<String>();
	    String key = (String)pObj.getKey().getValue().getValue();
	    OperationEnumeration op = pObj.getKey().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		keys.add(key);
		break;

	      case NOT_EQUAL:
		for (String value : listKeys(obj)) {
		    if (!key.equals(value)) {
			keys.add(value);
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = Pattern.compile(key);
		for (String value : listKeys(obj)) {
		    if (p.matcher(value).find()) {
			keys.add(value);
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	    for (String s : keys) {
		EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
		keyType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		keyType.setValue(s);
		values.put(Factories.sc.macos.createPlistItemKey(keyType), findValues(obj, s));
	    }
	}

	//
	// Create items for each key matching the object key spec
	//
	Collection<PlistItem> items = new ArrayList<PlistItem>();
	for (Map.Entry<JAXBElement<EntityItemStringType>, List<NSObject>> entry : values.entrySet()) {
	    int instance = 0;
	    OperationEnumeration op = null;
	    if (pObj.isSetInstance()) {
		instance = Integer.parseInt((String)pObj.getInstance().getValue());
		op = pObj.getInstance().getOperation();
	    }

	    Iterator<NSObject> iter = entry.getValue().iterator();
	    for (int i=1; iter.hasNext(); i++) {
		//
		// Determine whether, based on the instance number, the item should be returned as a result
		//
		boolean add = false;
		if (pObj.isSetInstance()) {
		    switch(op) {
		      case EQUALS:
			if (i == instance) {
			    add = true;
			}
			break;
		      case NOT_EQUAL:
			if (i != instance) {
			    add = true;
			}
			break;
		      case GREATER_THAN:
			if (i > instance) {
			    add = true;
			}
			break;
		      case GREATER_THAN_OR_EQUAL:
			if (i >= instance) {
			    add = true;
			}
			break;
		      case LESS_THAN:
			if (i < instance) {
			    add = true;
			}
			break;
		      case LESS_THAN_OR_EQUAL:
			if (i <= instance) {
			    add = true;
			}
			break;
		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    }
		} else {
		    add = true;
		}

		NSObject nso = iter.next();
		if (add) {
		    PlistItem item = Factories.sc.macos.createPlistItem();
		    item.setKey(entry.getKey());
		    item.setAppId(base.getAppId());
		    item.setFilepath(base.getFilepath());

		    EntityItemIntType instanceType = Factories.sc.core.createEntityItemIntType();
		    instanceType.setValue(Integer.toString(i));
		    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    item.setInstance(instanceType);

		    EntityItemPlistTypeType typeType = Factories.sc.macos.createEntityItemPlistTypeType();
		    typeType.setValue(getType(nso));
		    item.setType(typeType);

		    if (nso instanceof NSArray) {
			for (NSObject value : ((NSArray)nso).getArray()) {
			    try {
				item.getValue().add(toSimpleType(value));
			    } catch (IllegalArgumentException e) {
				item.setStatus(StatusEnumeration.ERROR);
				MessageType msg = Factories.common.createMessageType();
				msg.setLevel(MessageLevelEnumeration.ERROR);
				msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, e.getMessage()));
				item.getMessage().add(msg);
				EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
				valueType.setStatus(StatusEnumeration.ERROR);
				item.getValue().add(valueType);
				break;
			    }
			}
		    } else if (nso instanceof NSDictionary) {
			item.setStatus(StatusEnumeration.ERROR);
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, "CFDictionary"));
			item.getMessage().add(msg);
			EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
			valueType.setStatus(StatusEnumeration.ERROR);
			item.getValue().add(valueType);
		    } else {
			item.getValue().add(toSimpleType(nso));
		    }
		    items.add(item);
		}
	    }
	}
	return items;
    }

    // Private

    /**
     * Find all the [unique] keys in the Plist.
     */
    private Collection<String> listKeys(NSObject obj) {
	if (obj instanceof NSDictionary) {
	    Collection<String> keys = new HashSet<String>();
	    NSDictionary dict = (NSDictionary)obj;
	    for (String key : dict.allKeys()) {
		keys.add(key);
		NSObject child = dict.objectForKey(key);
		keys.addAll(listKeys(child));
	    }
	    return keys;
	} else {
	    @SuppressWarnings("unchecked")
	    Collection<String> empty = (Collection<String>)Collections.EMPTY_LIST;
	    return empty;
	}
    }

    /**
     * Recursively search an object for values with the given key, and return them in order.
     *
     * Note, this implementation here will drill down into dictionaries when they are keyed using something other than
     * the specified key, and search for the specified key recursively.  The OVAL specification does not document this
     * behavior, but it is the only interpretation that could produce multiple instances, etc.
     */
    private List<NSObject> findValues(NSObject obj, String key) {
	List<NSObject> values = new ArrayList<NSObject>();
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

    /**
     * Return the EntityItemPlistTypeType value corresponding to the NSObject.
     */
    private String getType(NSObject obj) {
	if (obj instanceof NSData) {
	    return "CFData";
	} else if (obj instanceof NSDate) {
	    return "CFDate";
	} else if (obj instanceof NSNumber) {
	    switch(((NSNumber)obj).type()) {
	      case NSNumber.BOOLEAN:
		return "CFBoolean";
	      default:
		return "CFNumber";
	    }
	} else if (obj instanceof NSArray) {
	    return "CFArray";
	} else if (obj instanceof NSDictionary) {
	    return "CFDictionary";
	}
	return "CFString";
    }

    /**
     * Return the simple type corresponding to the NSObject. Also, set the plist type in the type arg to the corresponding
     * type value.
     */
    private EntityItemAnySimpleType toSimpleType(NSObject obj) throws IllegalArgumentException {
	EntityItemAnySimpleType value = Factories.sc.core.createEntityItemAnySimpleType();
	if (obj instanceof NSData) {
	    value.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    value.setValue(((NSData)obj).getBase64EncodedData());
	} else if (obj instanceof NSDate) {
	    value.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    value.setValue(((NSDate)obj).toString());
	} else if (obj instanceof NSString) {
	    value.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    value.setValue(((NSString)obj).toString());
	} else if (obj instanceof NSNumber) {
	    NSNumber num = (NSNumber)obj;
	    switch(num.type()) {
	      case NSNumber.BOOLEAN:
		value.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		value.setValue(Boolean.toString(num.boolValue()));
		break;
	      case NSNumber.REAL:
		value.setDatatype(SimpleDatatypeEnumeration.FLOAT.value());
		value.setValue(Float.toString(num.floatValue()));
		break;
	      case NSNumber.INTEGER:
	      default:
		value.setDatatype(SimpleDatatypeEnumeration.INT.value());
		value.setValue(Double.toString(num.doubleValue()));
		break;
	    }
	} else {
	    throw new IllegalArgumentException(obj.getClass().toString());
	}
	return value;
    }
}
