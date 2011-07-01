// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe.resource;

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

    private byte[] buff;

    public ImageResourceDataEntry(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    public void debugPrint(PrintStream out) {
	out.println("IMAGE_RESOURCE_DATA_ENTRY:");
	out.println("  rawDataPtr: " + LittleEndian.toHexString(rawDataPtr));
	out.println("  size:       " + LittleEndian.toHexString(size));
	out.println("  codePage:   " + LittleEndian.toHexString(codePage));
	out.println("  reserved:   " + LittleEndian.toHexString(reserved));
    }

    /**
     * Return the absolute address (from the start of the file) to the data section of the resource.
     *
     * @param rba the address of the beginning of the resource section of the file (resource base address).
     * @param rva the RVA of the resource image directory
     */
    public long getDataAddress(long rba, long rva) {
	return rba + ((long)rawDataPtr) - rva;
    }

    // Private

    private void loadFromBuffer() {
	rawDataPtr	= LittleEndian.getUInt(buff, 0);
	size		= LittleEndian.getUInt(buff, 4);
	codePage	= LittleEndian.getUInt(buff, 8);
	reserved	= LittleEndian.getUInt(buff, 12);
    }
}
