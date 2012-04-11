// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.windows.LicenseObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryTypeType;
import oval.schemas.systemcharacteristics.windows.LicenseItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.registry.ILicenseData;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.LittleEndian;
import org.joval.oval.Factories;
import org.joval.oval.CollectException;
import org.joval.util.JOVALMsg;

/**
 * Provides access to Windows License data.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LicenseAdapter implements IAdapter {
    private IWindowsSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(LicenseObject.class);
	}
	return classes;
    }

    public Collection<LicenseItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<LicenseItem> items = new Vector<LicenseItem>();
	try {
	    LicenseObject lObj = (LicenseObject)obj;
	    IRegistry reg = null;
	    if (session.supports(IWindowsSession.View._64BIT)) {
		reg = session.getRegistry(IWindowsSession.View._64BIT);
	    } else {
		reg = session.getRegistry(IWindowsSession.View._32BIT);
	    }
	    Hashtable<String, ILicenseData.IEntry> entries = reg.getLicenseData().getEntries();
	    if (lObj.isSetName()) {
		JAXBElement<EntityObjectStringType> wrapped = ((LicenseObject)obj).getName();
		EntityObjectStringType name = wrapped.getValue();
		if (name != null && name.isSetValue()) {
		    String entryName = (String)name.getValue();
		    switch(name.getOperation()) {
		      case EQUALS:
			if (entries.containsKey(entryName)) {
			    items.add(getItem(entries.get(entryName)));
			}
			break;

		      case NOT_EQUAL:
			for (ILicenseData.IEntry entry : entries.values()) {
			    if (!entry.getName().equals(entryName)) {
				items.add(getItem(entry));
			    }
			}
			break;

		      case PATTERN_MATCH:
			for (ILicenseData.IEntry entry : entries.values()) {
			    if (Pattern.compile(entryName).matcher(entry.getName()).find()) {
				items.add(getItem(entry));
			    }
			}
			break;

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, name.getOperation());
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
		}
	    } else {
		for (ILicenseData.IEntry entry : entries.values()) {
		    items.add(getItem(entry));
		}
	    }
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    private LicenseItem getItem(ILicenseData.IEntry entry) {
	LicenseItem item = Factories.sc.windows.createLicenseItem();

	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(entry.getName());
	nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setName(Factories.sc.windows.createLicenseItemName(nameType));

	EntityItemRegistryTypeType registryType = Factories.sc.windows.createEntityItemRegistryTypeType();
	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	switch(entry.getType()) {
	  case ILicenseData.IEntry.TYPE_BINARY: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.BINARY.value());
	    byte[] data = ((ILicenseData.IBinaryEntry)entry).getData();
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < data.length; i++) {
		sb.append(LittleEndian.toHexString(data[i]));
	    }
	    valueType.setValue(sb.toString());
	    registryType.setValue("reg_binary");
	    break;
	  }
	  case ILicenseData.IEntry.TYPE_DWORD: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    valueType.setValue(Integer.toString(((ILicenseData.IDwordEntry)entry).getData()));
	    registryType.setValue("reg_dword");
	    break;
	  }
	  case ILicenseData.IEntry.TYPE_SZ: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    valueType.setValue(((ILicenseData.IStringEntry)entry).getData());
	    registryType.setValue("reg_sz");
	    break;
	  }
	}
	item.setType(registryType);
	item.setValue(valueType);

	return item;
    }
}
