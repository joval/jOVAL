// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.joval.io.LittleEndian;

public class StringFileInfo {
    public static final String KEY = "StringFileInfo";

    short length;
    short valueLength;
    short type;
    String key;
    byte[] padding;
    List <StringTable> children;

    StringFileInfo(short len, short vLen, short type, byte[] buff, int fileOffset) throws IOException {
	this.length = len;
	int end = length - 2; // length itself wasn't read from buff
	this.valueLength = vLen;
	this.type = type;
	int offset = 34; // short + short + sizeof("StringFileInfo\0")
	padding	= LittleEndian.get32BitAlignPadding(buff, offset, fileOffset);
	offset += padding.length;
	children = new Vector<StringTable>();
	while (offset < end) {
	    short childLength = LittleEndian.getUShort(buff, offset);
	    if (childLength > 0) {
		StringTable st = new StringTable(childLength, buff, offset, fileOffset);
		children.add(st);
		offset += childLength;
	    } else {
		break;
	    }
	}
    }

    public void debugPrint(PrintStream out) {
	out.println("STRINGFILEINFO:");
	out.println("  length:           " + LittleEndian.toHexString(length));
	out.println("  valueLength:      " + LittleEndian.toHexString(valueLength));
	out.println("  type:             " + LittleEndian.toHexString(type));
	out.println("  key:              " + KEY);
	out.print("  padding:          {");
	for (int i=0; i < padding.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding[i]));
	}
	out.println("}");
	Iterator <StringTable>iter = children.iterator();
	while(iter.hasNext()) {
	    iter.next().debugPrint(out);
	}
    }

    public List<StringTable> getChildren() {
	return children;
    }
}
