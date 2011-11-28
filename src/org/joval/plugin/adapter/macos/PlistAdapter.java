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
import oval.schemas.definitions.macos.PlistObject;
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
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.plugin.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Retrieves items for Plist objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PlistAdapter extends BaseFileAdapter {
    public PlistAdapter(ISession session) {
	super(session);
    }

    // Implement IAdapter

    private static Class[] objectClasses = {PlistObject.class};

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
		throws IOException, CollectionException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	PlistItem baseItem = (PlistItem)base;
	PlistObject pObj = (PlistObject)rc.getObject();

	if (baseItem != null && pObj != null && f.isFile()) {
	    PlistItem item = (PlistItem)createFileItem();
	    item.setFilepath(baseItem.getFilepath());

	    String appId = null;
	    if (pObj.isSetAppId()) {
		appId = (String)pObj.getAppId().getValue();
		if (appId != null) {
		    EntityItemStringType appIdType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    appIdType.setValue(appId);
		    item.setAppId(appIdType);
		}
	    }

	    String key = null;
	    if (pObj.isSetKey()) {
		key = (String)pObj.getKey().getValue().getValue();
		if (key != null) {
		    EntityItemStringType keyType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    keyType.setValue(key);
		    item.setKey(JOVALSystem.factories.sc.macos.createPlistItemKey(keyType));
		}
	    }

	    InputStream in = null;
	    try {
		in = f.getInputStream();
		NSObject obj = PropertyListParser.parse(in);

		if (key != null) {
		    NSDictionary dictionary = (NSDictionary)obj;
		    obj = dictionary.objectForKey(key);
		}

		EntityItemPlistTypeType typeType = JOVALSystem.factories.sc.macos.createEntityItemPlistTypeType();
		if (obj instanceof NSArray) {
		    typeType.setValue("CFArray");
		    NSObject[] array = ((NSArray)obj).getArray();
		    for (int i=0; i < array.length; i++) {
			EntityItemAnySimpleType val = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
			val.setValue(getValue(array[i]));
			item.getValue().add(val);
		    }
		} else {
		    if (obj instanceof NSData) {
			typeType.setValue("CFData");
		    } else if (obj instanceof NSDate) {
			typeType.setValue("CFDate");
		    } else if (obj instanceof NSNumber) {
			NSNumber num = (NSNumber)obj;
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
		    } else if (obj instanceof NSString) {
			typeType.setValue("CFString");
		    } else {
			String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, obj.getClass().getName());
			throw new CollectionException(msg);
		    }

		    EntityItemAnySimpleType val = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		    val.setValue(getValue(obj));
		    item.getValue().add(val);
		}
		item.setType(typeType);

		items.add(JOVALSystem.factories.sc.macos.createPlistItem(item));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_PARSE, f.getLocalName(), e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			JOVALSystem.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
		    }
		}
	    }
	}
	return items;
    }

    // Private

    private String getValue(NSObject obj) throws CollectionException {
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
	throw new CollectionException(JOVALSystem.getMessage(JOVALMsg.ERROR_PLIST_UNSUPPORTED_TYPE, obj.getClass().getName()));
    }
}
