// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.registry.IBinaryValue;
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.registry.IRegistry;
import jsaf.intf.windows.registry.IValue;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.io.LittleEndian;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.windowsx.LicenseObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.windows.EntityItemRegistryTypeType;
import scap.oval.systemcharacteristics.windowsx.LicenseItem;
import scap.oval.systemcharacteristics.windowsx.ObjectFactory;

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
    private ObjectFactory factory;
    private License license;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    factory = new ObjectFactory();
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
	    if (license == null) {
		license = new License(reg);
	    }
	    Map<String, License.Entry> entries = license.getEntries();
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
			for (License.Entry entry : entries.values()) {
			    if (!entry.getName().equals(entryName)) {
				items.add(getItem(entry));
			    }
			}
			break;

		      case PATTERN_MATCH:
			for (License.Entry entry : entries.values()) {
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
		for (License.Entry entry : entries.values()) {
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

    private LicenseItem getItem(License.Entry entry) {
	LicenseItem item = factory.createLicenseItem();

	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(entry.getName());
	nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	item.setName(nameType);

	EntityItemRegistryTypeType registryType = Factories.sc.windows.createEntityItemRegistryTypeType();
	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	switch(entry.getType()) {
	  case License.Entry.TYPE_BINARY: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.BINARY.value());
	    byte[] data = ((License.BinaryEntry)entry).getData();
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < data.length; i++) {
		sb.append(LittleEndian.toHexString(data[i]));
	    }
	    valueType.setValue(sb.toString());
	    registryType.setValue("reg_binary");
	    break;
	  }
	  case License.Entry.TYPE_DWORD: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    valueType.setValue(Integer.toString(((License.DwordEntry)entry).getData()));
	    registryType.setValue("reg_dword");
	    break;
	  }
	  case License.Entry.TYPE_SZ: {
	    valueType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    valueType.setValue(((License.StringEntry)entry).getData());
	    registryType.setValue("reg_sz");
	    break;
	  }
	}
	item.setType(registryType);
	item.setValue(valueType);

	return item;
    }

    /**
     * A class that can interpret a License registry key.
     * See http://www.geoffchappell.com/viewer.htm?doc=studies/windows/km/ntoskrnl/api/ex/slmem/productpolicy.htm
     */
    static class License {
	private Map<String, Entry> entries;

	License(IRegistry reg) throws Exception {
	    entries = new HashMap<String, Entry>();
	    IKey key = reg.getKey(IRegistry.Hive.HKLM, "SYSTEM\\CurrentControlSet\\Control\\ProductOptions");
	    IValue value = key.getValue("ProductPolicy");
	    switch(value.getType()) {
	      case REG_BINARY: {
		byte[] buff = ((IBinaryValue)value).getData();
		//
		// Read the header
		//
		int len		= LittleEndian.getUInt(buff, 0x00);
		int valueLen	= LittleEndian.getUInt(buff, 0x04);
		int endSize	= LittleEndian.getUInt(buff, 0x08);
		int junk	= LittleEndian.getUInt(buff, 0x0C);
		int version	= LittleEndian.getUInt(buff, 0x10);

		if (version == 1) {
		    if (len == buff.length) {
			int offset = 0x14;
			for (int bytesRead=0; bytesRead < valueLen; ) {
			    Entry entry = (Entry)readEntry(buff, offset);
			    entries.put(entry.getName(), entry);
			    bytesRead = bytesRead + entry.length();
			    offset = offset + entry.length();
			}
		    } else {
			throw new RuntimeException("Unexpected buffer length: " + buff.length + " (" + len + " expected)");
		    }
		} else {
		    throw new RuntimeException("Unexpected version number: " + version);
		}
		break;
	      }

	      default:
		throw new RuntimeException("Unexpected type: " + value.getType());
	    }
	}

	Map<String, Entry> getEntries() {
	    return entries;
	}

	// Private

	private Entry readEntry(byte[] buff, int offset) throws Exception {
	    short len		= LittleEndian.getUShort(buff, offset);
	    short nameLen	= LittleEndian.getUShort(buff, offset + 0x02);
	    short dataType	= LittleEndian.getUShort(buff, offset + 0x04);
	    short dataLen	= LittleEndian.getUShort(buff, offset + 0x06);
	    int flags		= LittleEndian.getUInt(buff, offset + 0x08);
	    int padding		= LittleEndian.getUInt(buff, offset + 0x0C);
	    String name		= LittleEndian.getSzUTF16LEString(buff, offset + 0x10, (int)nameLen);
	    byte[] data		= Arrays.copyOfRange(buff, offset + 0x10 + nameLen, offset + 0x10 + nameLen + dataLen);

	    switch(dataType) {
	      case Entry.TYPE_DWORD:
		if (dataLen == 4) {
		    return new DwordEntry(len, name, data);
		} else {
		    throw new RuntimeException("Illegal length for DWORD data: " + dataLen);
		}

	      case Entry.TYPE_SZ:
		return new StringEntry(len, name, data);

	      case Entry.TYPE_BINARY:
	      default:
		return new BinaryEntry(len, name, data);
	    }
	}

	static abstract class Entry {
	    static final int TYPE_SZ = 1;
	    static final int TYPE_BINARY = 2;
	    static final int TYPE_DWORD = 4;

	    private int len;

	    int dataType;
	    String name;

	    Entry(int len, int dataType, String name) {
		this.len = len;
		this.dataType = dataType;
		this.name = name;
	    }

	    int length() {
		return len;
	    }

	    int getType() {
		return dataType;
	    }

	    public String getName() {
		return name;
	    }
	}

	static class BinaryEntry extends Entry {
	    private byte[] data;

	    BinaryEntry(int len, String name, byte[] data) {
		super(len, TYPE_BINARY, name);
		this.data = data;
	    }

	    byte[] getData() {
		return data;
	    }

	    @Override
	    public String toString() {
		StringBuffer sb = new StringBuffer(name);
		sb.append(": BINARY: ");
		for (int i=0; i < data.length; i++) {
		    sb.append(LittleEndian.toHexString(data[i]));
		}
		return sb.toString();
	    }
	}

	static class DwordEntry extends Entry {
	    private int data;

	    DwordEntry(int len, String name, byte[] data) {
		super(len, TYPE_DWORD, name);
		this.data = LittleEndian.getUInt(data, 0);
	    }

	    int getData() {
		return data;
	    }

	    @Override
	    public String toString() {
		return name + ": DWORD: " + Integer.toHexString(data);
	    }
	}

	static class StringEntry extends Entry {
	    private String data;

	    StringEntry(int len, String name, byte[] data) {
		super(len, TYPE_SZ, name);
		this.data = LittleEndian.getSzUTF16LEString(data, 0, data.length);
	    }

	    String getData() {
		return data;
	    }

	    @Override
	    public String toString() {
		return name + ": String: " + data;
	    }
	}
    }
}
