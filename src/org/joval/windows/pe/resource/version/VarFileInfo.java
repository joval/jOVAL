// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;

import org.joval.io.LittleEndian;

public class VarFileInfo {
    public static final String KEY = "VarFileInfo";

    short length;
    short valueLength;
    short type;
    byte[] padding;
    Var children;

    VarFileInfo(short len, short vLen, short type, byte[] buff, int fileOffset) throws IOException {
	this.length		= len;
	this.valueLength	= vLen;
	this.type		= type;
	int offset = 28; // short + short + sizeof("VarFileInfo\0")
	padding			= LittleEndian.get32BitAlignPadding(buff, offset, fileOffset);
	offset += padding.length;
	children		= new Var(buff, offset, fileOffset);
    }

    public void debugPrint(PrintStream out) {
	out.println("VARFILEINFO:");
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
	children.debugPrint(out);
    }
}
