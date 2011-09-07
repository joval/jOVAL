// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

import java.util.Arrays;
import java.util.Hashtable;

import org.joval.intf.windows.registry.IBinaryValue;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IValue;
import org.joval.io.LittleEndian;

/**
 * A class that can interpret a License registry key.
 * See http://www.geoffchappell.com/viewer.htm?doc=studies/windows/km/ntoskrnl/api/ex/slmem/productpolicy.htm
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LicenseData {
    public static void main(String[] argv) {
	try {
	    LicenseData ld = new LicenseData(new org.joval.os.windows.registry.Registry());
	    Hashtable<String, Entry> ht = ld.getEntries();
	    for (Entry entry : ht.values()) {
		System.out.println(entry.toString());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private Hashtable<String, Entry> entries;

    public LicenseData(IRegistry reg) throws Exception {
	entries = new Hashtable<String, Entry>();
	IKey key = reg.fetchKey(IRegistry.HKLM + "\\SYSTEM\\CurrentControlSet\\Control\\ProductOptions");
	IValue value = reg.fetchValue(key, "ProductPolicy");
	switch(value.getType()) {
	  case IValue.REG_BINARY: {
	    byte[] buff = ((IBinaryValue)value).getData();
	    //
	    // Read the header
	    //
	    int len		= LittleEndian.getUInt(buff, 0x00);
	    int valueLen	= LittleEndian.getUInt(buff, 0x04);
	    int endSize		= LittleEndian.getUInt(buff, 0x08);
	    int junk		= LittleEndian.getUInt(buff, 0x0C);
	    int version		= LittleEndian.getUInt(buff, 0x10);

	    if (version == 1) {
		if (len == buff.length) {
		    int offset = 0x14;
		    for (int bytesRead=0; bytesRead < valueLen; ) {
			Entry entry = new Entry(buff, offset);
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

    Hashtable<String, Entry> getEntries() {
	return entries;
    }

    class Entry {
	static final int TYPE_SZ	= 1;
	static final int TYPE_BINARY	= 2;
	static final int TYPE_DWORD	= 4;

	short len;
	String name;
	short dataType;
	short dataLen;
	byte[] data;
	int flags;
	int padding;

	Entry(byte[] buff, int offset) {
	    len			= LittleEndian.getUShort(buff, offset);
	    short nameLen	= LittleEndian.getUShort(buff, offset + 0x02);
	    dataType		= LittleEndian.getUShort(buff, offset + 0x04);
	    dataLen		= LittleEndian.getUShort(buff, offset + 0x06);
	    flags		= LittleEndian.getUInt(buff, offset + 0x08);
	    padding		= LittleEndian.getUInt(buff, offset + 0x0C);
	    name		= LittleEndian.getSzUTF16LEString(buff, offset + 0x10, (int)nameLen);
	    data		= Arrays.copyOfRange(buff, offset + 0x10 + nameLen, offset + 0x10 + nameLen + dataLen);

	    switch(getType()) {
	      case TYPE_DWORD:
		if (dataLen != 4) {
		    throw new RuntimeException("Illegal length for DWORD data: " + dataLen);
		}
		break;
	    }
	}

	int length() {
	    return len;
	}

	int getType() {
	    return dataType;
	}

	String getName() {
	    return name;
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer(name);
	    sb.append(": ");

	    switch(getType()) {
	      case TYPE_SZ:
		sb.append("String: ");
		sb.append(LittleEndian.getSzUTF16LEString(data, 0, dataLen));
		break;

	      case TYPE_BINARY:
		sb.append("BINARY: ");
		for (int i=0; i < data.length; i++) {
		    sb.append(LittleEndian.toHexString(data[i]));
		}
		break;

	      case TYPE_DWORD:
		sb.append("DWORD=").append(Integer.toHexString(LittleEndian.getUInt(data, 0)));
		break;

	      default:
		sb.append("Unknown type ").append(dataType);
		sb.append(" RAW: ");
		for (int i=0; i < data.length; i++) {
		    sb.append(LittleEndian.toHexString(data[i]));
		}
		break;
	    }

	    return sb.toString();
	}
    }
}
