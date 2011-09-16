// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.joval.io.StreamTool;
import org.joval.io.LittleEndian;
import org.joval.util.JOVALSystem;

/**
 * See http://msdn.microsoft.com/en-us/library/ms680341%28VS.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageDataDirectory {
    public static final int BUFFER_SIZE = 8;

    public static final String[] TABLE_NAMES = {"EXPORT",
    						"IMPORT",
						"RESOURCE",
						"EXCEPTION",
						"SECURITY",
						"BASERELOC",
						"DEBUG",
						"ARCHITECTURE",
						"GLOBALPTR",
						"TLS",
						"LOAD_CONFIG",
						"BOUND_IMPORT",	// These two entries added for NT 3.51
						"IAT",
						"DELAY_IMPORT",	// This entry added in NT 5 time frame
						"COM_DESCRPTR"};// For the .NET runtime (previously called COM+ 2.0)

    public static final int IMAGE_NUMBEROF_DIRECTORY_ENTRIES = TABLE_NAMES.length;

    public static final int EXPORT_TABLE			= 0;
    public static final int IMPORT_TABLE			= 1;
    public static final int RESOURCE_TABLE			= 2;
    public static final int EXCEPTION_TABLE			= 3;
    public static final int CERTIFICATE_TABLE			= 4;
    public static final int BASE_RELOCATION_TABLE		= 5;
    public static final int DEBUG_TABLE				= 6;
    public static final int ARCHITECTURE_TABLE			= 7;
    public static final int GLOBAL_POINTER_REGISTER_TABLE	= 8;
    public static final int THREAD_LOCAL_STORAGE_TABLE		= 9;
    public static final int LOAD_CONFIG_TABLE			= 10;
    public static final int BOUND_IMPORT_TABLE			= 11;
    public static final int IMPORT_ADDRESS_TABLE		= 12;
    public static final int DELAY_IMPORT_DESCRIPTOR_TABLE	= 13;
    public static final int CLR_TABLE				= 14;

    int    virtualAddress;
    int    size;

    public ImageDataDirectory(byte[] buff, int offset) {
	virtualAddress	= LittleEndian.getUInt(buff, offset);
	size		= LittleEndian.getUInt(buff, offset + 4);
    }

    public void debugPrint(PrintStream out, int index) {
	out.println("  " + TABLE_NAMES[index]);
	out.println("    virtualAddress: " + LittleEndian.toHexString(virtualAddress));
	out.println("    size:           " + LittleEndian.toHexString(size));
    }

    public long getAddress() {
	return (long)virtualAddress;
    }

    public int getSize() {
	return size;
    }

}
