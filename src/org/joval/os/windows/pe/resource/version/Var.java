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
    List<LangAndCodepage> children;

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
	children	= new Vector<LangAndCodepage>();
	while (offset < end) {
	    short langId = LittleEndian.getUShort(buff, offset);
	    offset += 2;
	    short codepage = LittleEndian.getUShort(buff, offset);
	    offset += 2;
	    children.add(new LangAndCodepage(langId, codepage));
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
	for (LangAndCodepage lac : children) {
	    out.print(indent);
	    out.println("Child[" + i++ + "]: " + lac.langcode + lac.codepage);
	}
    }

    public class LangAndCodepage {
	String langcode, codepage;

	LangAndCodepage(short langcode, short codepage) {
	    this.langcode = String.format("%04x", langcode);
	    this.codepage = String.format("%04x", codepage);
	}

	public String getLangCode() {
	    return langcode;
	}

	public String getCodepage() {
	    return codepage;
	}

	public String toString() {
	    return new StringBuffer(langcode).append(codepage).toString();
	}
    }
}
