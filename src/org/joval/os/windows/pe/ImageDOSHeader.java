// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Reads the first 64-bytes of a Portable Executable (PE) format-file, which is the MS-DOS header.
 * See http://msdn.microsoft.com/en-us/library/ms809762.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageDOSHeader {
    public static final int BUFFER_SIZE = 64;

    short   e_magic;      // Magic number
    short   e_cblp;       // Bytes on last page of file
    short   e_cp;         // Pages in file
    short   e_crlc;       // Relocations
    short   e_cparhdr;    // Size of header in paragraphs
    short   e_minalloc;   // Minimum extra paragraphs needed
    short   e_maxalloc;   // Maximum extra paragraphs needed
    short   e_ss;         // Initial (relative) SS value
    short   e_sp;         // Initial SP value
    short   e_csum;       // Checksum
    short   e_ip;         // Initial IP value
    short   e_cs;         // Initial (relative) CS value
    short   e_lfarlc;     // File address of relocation table
    short   e_ovno;       // Overlay number
    short[] e_res;        // Reserved words
    short   e_oemid;      // OEM identifier (for e_oeminfo)
    short   e_oeminfo;    // OEM information; e_oemid specific
    short[] e_res2;       // Reserved words
    int     e_lfanew;     // File address of new exe header

    private byte[] buff;

    /**
     * Read the IMAGE_DOS_HEADER from the first 64 bytes of the InputStream.
     */
    public ImageDOSHeader(InputStream in) throws IOException {
	buff = new byte[BUFFER_SIZE];
	StreamTool.readFully(in, buff);
	loadFromBuffer();
    }

    public ImageDOSHeader(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    /**
     * Get the number of bytes to skip to advance to the NT header block.
     */
    public int getELFHeaderRVA() {
	return e_lfanew;
    }

    public void debugPrint(PrintStream out) {
	out.println("RAW Buffer for IMAGE_DOS_HEADER:");
	StreamTool.hexDump(buff, out);

	out.println("IMAGE_DOS_HEADER:");
	out.println("  e_magic:    " + LittleEndian.toHexString(e_magic));
	out.println("  e_cblp:     " + LittleEndian.toHexString(e_cblp));
	out.println("  e_cp:       " + LittleEndian.toHexString(e_cp));
	out.println("  e_crlc:     " + LittleEndian.toHexString(e_crlc));
	out.println("  e_cparhdr:  " + LittleEndian.toHexString(e_cparhdr));
	out.println("  e_minalloc: " + LittleEndian.toHexString(e_minalloc));
	out.println("  e_maxalloc: " + LittleEndian.toHexString(e_maxalloc));
	out.println("  e_ss:       " + LittleEndian.toHexString(e_ss));
	out.println("  e_sp:       " + LittleEndian.toHexString(e_sp));
	out.println("  e_csum:     " + LittleEndian.toHexString(e_csum));
	out.println("  e_ip:       " + LittleEndian.toHexString(e_ip));
	out.println("  e_cs:       " + LittleEndian.toHexString(e_cs));
	out.println("  e_lfarlc:   " + LittleEndian.toHexString(e_lfarlc));
	out.println("  e_ovno:     " + LittleEndian.toHexString(e_ovno));
	out.print("  e_res:      {");
	for (int i=0; i < e_res.length; i++) {
	    if (i > 0) out.print(", ");
	    out.print(LittleEndian.toHexString(e_res[i]));
	}
	out.println("}");
	out.println("  e_oemid:    " + LittleEndian.toHexString(e_oemid));
	out.println("  e_oeminfo:  " + LittleEndian.toHexString(e_oeminfo));
	out.print("  e_res2:     {");
	for (int i=0; i < e_res2.length; i++) {
	    if (i > 0) out.print(", ");
	    out.print(LittleEndian.toHexString(e_res2[i]));
	}
	out.println("}");
	out.println("  e_lfanew:   " + LittleEndian.toHexString(e_lfanew));
    }

    // Private

    private void loadFromBuffer() {
	if (buff.length != BUFFER_SIZE) {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_WINPE_BUFFERLEN, buff.length);
	    throw new IllegalArgumentException(s);
	}

	e_magic		= LittleEndian.getUShort(buff, 0);
	if (e_magic != 0x5A4D) { // MZ
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_WINPE_MAGIC, Integer.toHexString(e_magic));
	    throw new IllegalArgumentException(s);
	}
	e_cblp		= LittleEndian.getUShort(buff, 2);
	e_cp		= LittleEndian.getUShort(buff, 4);
	e_crlc		= LittleEndian.getUShort(buff, 6);
	e_cparhdr	= LittleEndian.getUShort(buff, 8);
	e_minalloc	= LittleEndian.getUShort(buff, 10);
	e_maxalloc	= LittleEndian.getUShort(buff, 12);
	e_ss		= LittleEndian.getUShort(buff, 14);
	e_sp		= LittleEndian.getUShort(buff, 16);
	e_csum		= LittleEndian.getUShort(buff, 18);
	e_ip		= LittleEndian.getUShort(buff, 20);
	e_cs		= LittleEndian.getUShort(buff, 22);
	e_lfarlc	= LittleEndian.getUShort(buff, 24);
	e_ovno		= LittleEndian.getUShort(buff, 26);

	e_res = new short[4];
	e_res[0]	= LittleEndian.getUShort(buff, 28);
	e_res[1]	= LittleEndian.getUShort(buff, 30);
	e_res[2]	= LittleEndian.getUShort(buff, 32);
	e_res[3]	= LittleEndian.getUShort(buff, 34);
	e_oemid		= LittleEndian.getUShort(buff, 36);
	e_oeminfo	= LittleEndian.getUShort(buff, 38);

	e_res2 = new short[10];
	e_res2[0]	= LittleEndian.getUShort(buff, 40);
	e_res2[1]	= LittleEndian.getUShort(buff, 42);
	e_res2[2]	= LittleEndian.getUShort(buff, 44);
	e_res2[3]	= LittleEndian.getUShort(buff, 46);
	e_res2[4]	= LittleEndian.getUShort(buff, 48);
	e_res2[5]	= LittleEndian.getUShort(buff, 50);
	e_res2[6]	= LittleEndian.getUShort(buff, 52);
	e_res2[7]	= LittleEndian.getUShort(buff, 54);
	e_res2[8]	= LittleEndian.getUShort(buff, 56);
	e_res2[9]	= LittleEndian.getUShort(buff, 58);
	e_lfanew	= LittleEndian.getUInt(buff, 60);
    }
}
