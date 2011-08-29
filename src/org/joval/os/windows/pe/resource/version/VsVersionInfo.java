// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;
import org.joval.io.StreamTool;
import org.joval.util.JOVALSystem;

/**
 * VsVersionInfo data structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VsVersionInfo {
    public static final String LANGID_KEY		= "040904B0";
    public static final String STRINGFILEINFO_KEY	= "StringFileInfo";
    public static final String TRANSLATION_KEY		= "Translation";
    public static final String VARFILEINFO_KEY		= "VarFileInfo";
    public static final String VSVERSIONINFO_KEY	= "VS_VERSION_INFO";

    short  length;
    short  valueLength;
    short  type;
    String key;
    byte[] padding1;
    VsFixedFileInfo value;
    byte[] padding2;
    List <Object> children;

    public VsVersionInfo(IRandomAccess ra) throws IOException {
	length		= LittleEndian.readUShort(ra);
	valueLength	= LittleEndian.readUShort(ra);
	type		= LittleEndian.readUShort(ra);
	key		= LittleEndian.readSzUTF16LEString(ra);

	//
	// Padding is used to 32-bit align the Value data structure.
	//
	padding1 = LittleEndian.read32BitAlignPadding(ra);
	if (valueLength > 0) {
	    byte[] buff = new byte[valueLength];
	    ra.readFully(buff);
	    value = new VsFixedFileInfo(buff);
	}
//	padding2 = LittleEndian.read32BitAlignPadding(ra);

	children = new Vector<Object>();
	for (int i=0; i < 2; i++) {
	    short childLength = LittleEndian.readUShort(ra);
	    if (childLength == 0) {
		break;
	    }
	    byte[] childBuff = new byte[childLength - 2];
	    int fileOffset = (int)ra.getFilePointer();
	    ra.readFully(childBuff);
	    short childValueLength = LittleEndian.getUShort(childBuff, 0);
	    short childType = LittleEndian.getUShort(childBuff, 2);
	    String childKey = LittleEndian.getSzUTF16LEString(childBuff, 4, -1);
	    if (StringFileInfo.KEY.equals(childKey)) {
		children.add(new StringFileInfo(childLength, childValueLength, childType, childBuff, fileOffset));
	    } else if (VarFileInfo.KEY.equals(childKey)) {
		children.add(new VarFileInfo(childLength, childValueLength, childType, childBuff, fileOffset));
	    } else {
		throw new IOException(JOVALSystem.getMessage("ERROR_WINPE_VSVKEY", childKey));
	    }
	}
    }

    public void debugPrint(PrintStream out) {
	out.println("VERSION_INFO:");
	out.println("  length:           " + LittleEndian.toHexString(length));
	out.println("  valueLength:      " + LittleEndian.toHexString(valueLength));
	out.println("  type:             " + LittleEndian.toHexString(type));
	out.println("  key:              " + key);
	out.print("  padding1:         {");
	for (int i=0; i < padding1.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding1[i]));
	}
	out.println("}");
	if (value != null) {
	    value.debugPrint(out);
	}
/*
	out.print("  padding2:         {");
	for (int i=0; i < padding2.length; i++) {
	    if (i > 0) {
		out.print(", ");
	    }
 	    out.print(LittleEndian.toHexString(padding1[i]));
	}
*/
	Iterator <Object>iter = children.iterator();
	while (iter.hasNext()) {
	    Object obj = iter.next();
	    if (obj instanceof StringFileInfo) {
		((StringFileInfo)obj).debugPrint(out);
	    } else if (obj instanceof VarFileInfo) {
		((VarFileInfo)obj).debugPrint(out);
	    }
	}
    }

    public VsFixedFileInfo getValue() {
	return value;
    }

    /**
     * Returns a list with elements of type StringFileInfo and VarFileInfo.
     */
    public List<Object>getChildren() {
	return children;
    }
}
