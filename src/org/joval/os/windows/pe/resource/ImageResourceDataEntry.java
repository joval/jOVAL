// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource;

import java.io.IOException;
import java.io.PrintStream;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;

/**
 * ImageResourceDataEntry structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageResourceDataEntry {
    public static final int BUFFER_SIZE = 16;

    int rawDataPtr;
    int size;
    int codePage;
    int reserved;
    long address = 0;

    private byte[] buff;

    /**
     * @param rba the address of the beginning of the resource section of the file (resource base address).
     * @param rva the RVA of the resource image directory
     */
    ImageResourceDataEntry(IRandomAccess ra, long rba, long rva) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
	address = rba + ((long)rawDataPtr) - rva;
    }

    public void debugPrint(PrintStream out, int level) {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < level; i++) {
	    sb.append("  ");
	}
	String indent = sb.toString();

	out.print(indent);
	out.println("rawDataPtr: " + LittleEndian.toHexString(rawDataPtr));
	out.print(indent);
	out.println("size:       " + LittleEndian.toHexString(size));
	out.print(indent);
	out.println("codePage:   " + LittleEndian.toHexString(codePage));
	out.print(indent);
	out.println("reserved:   " + LittleEndian.toHexString(reserved));
    }

    public long getDataAddress() {
	return address;
    }

    // Private

    private void loadFromBuffer() {
	rawDataPtr	= LittleEndian.getUInt(buff, 0);
	size		= LittleEndian.getUInt(buff, 4);
	codePage	= LittleEndian.getUInt(buff, 8);
	reserved	= LittleEndian.getUInt(buff, 12);
    }
}
