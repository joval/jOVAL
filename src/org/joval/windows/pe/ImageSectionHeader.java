// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe;

import java.io.IOException;
import java.io.PrintStream;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;
import org.joval.io.StreamTool;
import org.joval.util.JOVALSystem;

/**
 * See http://msdn.microsoft.com/en-us/library/ms680341%28VS.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageSectionHeader {
    public static final int BUFFER_SIZE = 40;
    public static final int IMAGE_SIZEOF_SHORT_NAME = 8;

    String name;
    int    virtualSize;
    int    virtualAddress;
    int    sizeOfRawData;
    int    pointerToRawData;
    int    pointerToRelocations;
    int    pointerToLinenumbers;
    short  numberOfRelocations;
    short  numberOfLinenumbers;
    int    characteristics;

    private byte[] buff;

    public ImageSectionHeader(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    public void debugPrint(PrintStream out) {
	out.println("RAW Buffer for IMAGE_SECTION_HEADER:");
	StreamTool.hexDump(buff, out);

	out.println("IMAGE_SECTION_HEADER:");
	out.println("  name:                 " + name);
	out.println("  virtualSize:          " + LittleEndian.toHexString(virtualSize));
	out.println("  virtualAddress:       " + LittleEndian.toHexString(virtualAddress));
	out.println("  sizeOfRawData:        " + LittleEndian.toHexString(sizeOfRawData));
	out.println("  pointerToRawData:     " + LittleEndian.toHexString(pointerToRawData));
	out.println("  pointerToRelocations: " + LittleEndian.toHexString(pointerToRelocations));
	out.println("  pointerToLinenumbers: " + LittleEndian.toHexString(pointerToLinenumbers));
	out.println("  numberOfRelocations:  " + LittleEndian.toHexString(numberOfRelocations));
	out.println("  numberOfLinenumbers:  " + LittleEndian.toHexString(numberOfLinenumbers));
	out.println("  characteristics:      " + LittleEndian.toHexString(characteristics));
    }

    public long getRawDataPosition() {
	return (long)pointerToRawData;
    }

    // Private

    private void loadFromBuffer() {
	if (buff == null) {
	    throw new IllegalArgumentException("Cannot load a header from a null buffer!");
	} else if (buff.length != BUFFER_SIZE) {
	    throw new IllegalArgumentException("Bad buffer length: " + buff.length);
	}

/*
	int pos = 0;
	for (; i < IMAGE_SIZEOF_SHORT_NAME; i++) {
	    if (buff[pos] == 0) {
		break;
	    }
	}
*/
	try {
	    name = new String(buff, 0, IMAGE_SIZEOF_SHORT_NAME, Charset.forName("UTF-8"));
	} catch (IllegalCharsetNameException e) {
	    e.printStackTrace();
	} catch (UnsupportedCharsetException e) {
	    e.printStackTrace();
	}

	virtualSize		= LittleEndian.getUInt(buff, 8);
	virtualAddress		= LittleEndian.getUInt(buff, 12);
	sizeOfRawData		= LittleEndian.getUInt(buff, 16);
	pointerToRawData	= LittleEndian.getUInt(buff, 20);
	pointerToRelocations	= LittleEndian.getUInt(buff, 24);
	pointerToLinenumbers	= LittleEndian.getUInt(buff, 28);
	numberOfRelocations	= LittleEndian.getUShort(buff, 32);
	numberOfLinenumbers	= LittleEndian.getUShort(buff, 34);
	characteristics		= LittleEndian.getUInt(buff, 36);
    }
}
