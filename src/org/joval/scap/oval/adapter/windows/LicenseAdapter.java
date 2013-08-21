// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.registry.ILicenseData;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.io.LittleEndian;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.windows.LicenseObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.windows.EntityItemRegistryTypeType;
import scap.oval.systemcharacteristics.windows.LicenseItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.CollectException;
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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(LicenseObject.class);
	} else {
	    notapplicable.add(LicenseObject.class);
	}
	return classes;
    }

    public Collection<LicenseItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	Collection<LicenseItem> items = new ArrayList<LicenseItem>();
	try {
	    LicenseObject lObj = (LicenseObject)obj;
	    IRegistry reg = null;
	    if (session.supports(IWindowsSession.View._64BIT)) {
		reg = session.getRegistry(IWindowsSession.View._64BIT);
	    } else {
		reg = session.getRegistry(IWindowsSession.View._32BIT);
	    }
	    Map<String, ILicenseData.IEntry> entries = reg.getLicenseData().getEntries();
	    if (lObj.isSetName()) {
		EntityObjectStringType name = lObj.getName();
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
			    if (StringTools.pattern(entryName).matcher(entry.getName()).find()) {
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
	item.setName(nameType);

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
