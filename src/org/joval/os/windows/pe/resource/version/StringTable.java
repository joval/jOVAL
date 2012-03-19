// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.joval.io.LittleEndian;
import org.joval.util.JOVALMsg;

public class StringTable {
    short length;
    short valueLength;
    short type;
    String key;
    byte[] padding;
    List<StringStructure> children;

    StringTable(short length, byte[] buff, int offset, int fileOffset) throws IOException {
	this.length = length;
	int end = offset + length; // this is the index of the last byte in the buffer that's part of the table
	if (end >= buff.length) {
	    end = buff.length - 1;
	}
	offset += 2;	// length of length was not added to offset when initialized
	valueLength = LittleEndian.getUShort(buff, offset);
	offset += 2;
	type = LittleEndian.getUShort(buff, offset);
	offset += 2;
	key = LittleEndian.getSzUTF16LEString(buff, offset, -1);
	offset += 18; // 8 double-byte chars plus a double-byte null
	padding = LittleEndian.get32BitAlignPadding(buff, offset, fileOffset);
	offset += padding.length;
	children = new Vector<StringStructure>();
	while (offset < end) {
	    StringStructure str = new StringStructure(buff, offset, fileOffset);
	    children.add(str);
	    offset += str.length;
	    if (str.length == 0) {
		throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_WINPE_STRSTR0LEN));
	    }

	    //
	    // There appears to be undocumented random whitespace inside of StringTables between StringStructures.
	    //
	    while (offset < end) {
	 	byte b1 = buff[offset];
	 	byte b2 = buff[offset+1];
		if (b1 == 0 && b2 == 0) {
		    offset += 2;
		} else {
		    break;
		}
	    }
	}
    }

    public void debugPrint(PrintStream out, int level) {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < level; i++) {
	    sb.append("  ");
	}
	String indent = sb.toString();

	out.print(indent);
	out.println("length:           " + LittleEndian.toHexString(length));
	out.print(indent);
	out.println("valueLength:      " + LittleEndian.toHexString(valueLength));
	out.print(indent);
	out.println("type:             " + LittleEndian.toHexString(type));
	out.print(indent);
	out.println("key:              " + key);
	out.print(indent);
	out.print("padding:          {");
	for (int i=0; i < padding.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding[i]));
	}
	out.println("}");
	int i=0;
	for (StringStructure string : children) {
	    out.print(indent);
	    out.println("child[" + i++ + "]: {");
	    string.debugPrint(out, level + 1);
	    out.print(indent);
	    out.println("}");
	}
    }

    public String getKey() {
	return key;
    }

    public List<StringStructure> getChildren() {
	return children;
    }
}
