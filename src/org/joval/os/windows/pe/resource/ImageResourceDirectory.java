// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource;

import java.io.IOException;
import java.io.PrintStream;

import java.util.Date;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;

/**
 * ImageResourceDirectory data structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageResourceDirectory {
    public static final int BUFFER_SIZE = 16;

    int   characteristics;
    long  timeDateStamp;
    short majorVersion;
    short minorVersion;
    short numberOfNamedEntries;
    short numberOfIdEntries;
    ImageResourceDirectoryEntry[] entries;

    private byte[] buff = new byte[BUFFER_SIZE];

    public ImageResourceDirectory(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
	entries = getChildren(ra);
    }

    public int numEntries() {
	return numberOfNamedEntries + numberOfIdEntries;
    }

    public void debugPrint(PrintStream out, int level) {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < level; i++) {
	    sb.append("  ");
	}
	String indent = sb.toString();

	out.print(indent);
	out.println("characteristics:      " + LittleEndian.toHexString(characteristics));
	out.print(indent);
	out.println("timeDateStamp:        " + new Date(timeDateStamp).toString());
	out.print(indent);
	out.println("majorVersion:         " + LittleEndian.toHexString(majorVersion));
	out.print(indent);
	out.println("minorVersion:         " + LittleEndian.toHexString(minorVersion));
	out.print(indent);
	out.println("numberOfNamedEntries: " + LittleEndian.toHexString(numberOfNamedEntries));
	out.print(indent);
	out.println("numberOfIdEntries:    " + LittleEndian.toHexString(numberOfIdEntries));
	for (int i=0; i < entries.length; i++) {
	    out.print(indent);
	    out.print("entry[" + i + "]: {");
	    entries[i].debugPrint(out, level + 1);
	    out.print(indent);
	    out.println("}");
	}
    }

    public ImageResourceDirectoryEntry[] getChildEntries() {
	return entries;
    }

    // Private

    void loadFromBuffer() throws IOException {
	characteristics		= LittleEndian.getUInt(buff, 0);
	timeDateStamp		= (long)(LittleEndian.getUInt(buff, 4) * 1000);
	majorVersion		= LittleEndian.getUShort(buff, 8);
	minorVersion		= LittleEndian.getUShort(buff, 10);
	numberOfNamedEntries	= LittleEndian.getUShort(buff, 12);
	numberOfIdEntries	= LittleEndian.getUShort(buff, 14);
    }

    ImageResourceDirectoryEntry[] getChildren(IRandomAccess ra) throws IOException {
	int size = numEntries();
	ImageResourceDirectoryEntry[] entries = new ImageResourceDirectoryEntry[size];
	for (int i=0; i < size; i++) {
	    entries[i] = new ImageResourceDirectoryEntry(ra);
	}
	return entries;
    }
}
