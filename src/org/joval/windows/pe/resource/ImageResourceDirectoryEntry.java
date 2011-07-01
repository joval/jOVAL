// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe.resource;

import java.io.IOException;
import java.io.PrintStream;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;

/**
 * ImageResourceDirectoryEntry data structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageResourceDirectoryEntry {
    public static final int BUFFER_SIZE				= 8;
    public static final int OFFSET_MASK				= 0x7FFFFFFF;
    public static final int IMAGE_RESOURCE_DATA_IS_DIRECTORY	= 0x80000000;
    public static final int IMAGE_RESOURCE_NAME_IS_STRING	= 0x80000000;

    /**
     * This field contains either an integer ID or a pointer to a structure that contains a string name. If the high bit
     * (0x80000000) is zero, this field is interpreted as an integer ID. If the high bit is nonzero, the lower 31 bits are an
     * offset (relative to the start of the resources) to an IMAGE_RESOURCE_DIR_STRING_U structure. This structure contains a
     * WORD character count, followed by a UNICODE string with the resource name. Yes, even PE files intended for non-UNICODE
     * Win32 implementations use UNICODE here. To convert the UNICODE string to an ANSI string, use the WideCharToMultiByte
     * function.
     */
    int name;

    /**
     * This field is either an offset to another resource directory or a pointer to information about a specific resource
     * instance. If the high bit (0x80000000) is set, this directory entry refers to a subdirectory. The lower 31 bits are an
     * offset (relative to the start of the resources) to another IMAGE_RESOURCE_DIRECTORY. If the high bit isn't set, the
     * lower 31 bits point to an IMAGE_RESOURCE_DATA_ENTRY structure. The IMAGE_RESOURCE_DATA_ENTRY structure contains the
     * location of the resource's raw data, its size, and its code page.
     */
    int dataOffset;

    private byte[] buff;

    public ImageResourceDirectoryEntry(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    public void debugPrint(PrintStream out) {
	out.println("IMAGE_RESOURCE_DIRECTORY_ENTRY:");
	out.println("  name:       " + LittleEndian.toHexString(name));
	out.println("  dataOffset: " + LittleEndian.toHexString(dataOffset));
	out.println("  isDir:      " + isDir());
	out.println("  offset:     " + Integer.toHexString(getOffset()));
    }

    public boolean isDir() {
	return 0 != (IMAGE_RESOURCE_DATA_IS_DIRECTORY & dataOffset);
    }

    /**
     * Get the offset from the beginning of the section to this resource.
     */
    public int getOffset() {
	return OFFSET_MASK & dataOffset;
    }

    public boolean hasDataEntry() {
	return !isDir();
    }

    public ImageResourceDataEntry getDataEntry(IRandomAccess ra, long rba) throws IOException {
	if (hasDataEntry()) {
	    ra.seek(rba + (long)getOffset());
	    return new ImageResourceDataEntry(ra);
	}
	return null;
    }

    public int getType() {
	return name;
    }

    /**
     * The Name field stores an integer ID if its high bit is 0, or an offset (in the lower 31 bits) to an
     * IMAGE_RESOURCE_DIR_STRING_U structure if its high bit is 1. The offset is relative to the start of the resource
     * section, and this structure identifies a Unicode string that names a resource instance.
     */
    public String getName(IRandomAccess ra, long resourceBaseAddress) throws IOException {
	if (0 != (name & IMAGE_RESOURCE_NAME_IS_STRING)) {
	    //
	    // High bit is 1
	    //
	    int offset = name & OFFSET_MASK;
	    ra.seek((long)(resourceBaseAddress + offset));
	    int len = (int)LittleEndian.readUShort(ra);
	    byte[] buff = new byte[(len * 2)]; // UTF-8 chars are 16 bits = 2 bytes
	    for (int i=0; i < buff.length; i++) {
		buff[i] = (byte)ra.read();
	    }
	    try {
		return new String(buff, Charset.forName("UTF-8"));
	    } catch (UnsupportedCharsetException e) {
		e.printStackTrace();
	    } catch (IllegalCharsetNameException e) {
		e.printStackTrace();
	    } catch (IllegalArgumentException e) {
		e.printStackTrace();
	    }
	}
	//
	// High bit is 0, or there was a bizarre Charset error.
	//
	return LittleEndian.toHexString(name);
    }

    // Private

    private void loadFromBuffer() {
	name		= LittleEndian.getUInt(buff, 0);
	dataOffset	= LittleEndian.getUInt(buff, 4);
    }
}
