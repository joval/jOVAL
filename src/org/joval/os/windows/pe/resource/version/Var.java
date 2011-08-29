// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.joval.io.LittleEndian;

public class Var {
    short length;
    short valueLength;
    short type;
    String key;
    byte[] padding;
    List<Integer> children;

    Var(byte[] buff, int offset, int fileOffset) throws IOException {
	int start	= offset;
	length		= LittleEndian.getUShort(buff, offset);
	offset += 2;
	int end		= start + length;
	valueLength	= LittleEndian.getUShort(buff, offset);
	offset += 2;
	type		= LittleEndian.getUShort(buff, offset);
	offset += 2;
	key		= LittleEndian.getSzUTF16LEString(buff, offset, -1);
	offset += key.length() + 2;
	padding		= LittleEndian.get32BitAlignPadding(buff, offset, fileOffset);
	offset += padding.length;
	children	= new Vector<Integer>();
	while (offset < end) {
	    children.add(new Integer(LittleEndian.getUShort(buff, offset)));
	    offset += 2;
	}
    }

    public void debugPrint(PrintStream out) {
	out.println("VAR:");
	out.println("  length:           " + LittleEndian.toHexString(length));
	out.println("  valueLength:      " + LittleEndian.toHexString(valueLength));
	out.println("  type:             " + LittleEndian.toHexString(type));
	out.println("  key:              " + key);
	out.print("  padding:          {");
	for (int i=0; i < padding.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding[i]));
	}
	out.println("}");
	Iterator <Integer>iter = children.iterator();
	for (int i=0; iter.hasNext(); i++) {
	    out.println("Child[" + i + "]: " + Integer.toHexString(iter.next().intValue()));
	}
    }
}
