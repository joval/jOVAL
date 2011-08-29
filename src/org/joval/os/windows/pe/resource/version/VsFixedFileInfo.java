// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe.resource.version;

import java.io.IOException;
import java.io.PrintStream;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;
import org.joval.io.StreamTool;
import org.joval.util.Version;

/**
 * VsFixedFileInfo data structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VsFixedFileInfo {
    public final static int BUFFER_SIZE = 52;

    int   signature;
    int   strucVersion;
    int   fileVersionMS;
    int   fileVersionLS;
    int   productVersionMS;
    int   productVersionLS;
    int   fileFlagsMask;
    int   fileFlags;
    int   fileOS;
    int   fileType;
    int   fileSubtype;
    int   fileDateMS;
    int   fileDateLS;

    private byte[] buff;

    VsFixedFileInfo(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    VsFixedFileInfo(byte[] buff) throws IOException {
	if (buff.length != BUFFER_SIZE) {
	    throw new IOException("Invalid buffer size: " + buff.length);
	}
	this.buff = buff;
	loadFromBuffer();
    }

    public void debugPrint(PrintStream out) {
	StreamTool.hexDump(buff, out);
	out.println("VS_FIXEDFILEINFO:");
	out.println("  signature:        " + LittleEndian.toHexString(signature));
	out.println("  strucVersion:     " + LittleEndian.toHexString(strucVersion));
	out.println("  fileVersionMS:    " + LittleEndian.toHexString(fileVersionMS));
	out.println("  fileVersionLS:    " + LittleEndian.toHexString(fileVersionLS));
	out.println("  productVersionMS: " + LittleEndian.toHexString(productVersionMS));
	out.println("  productVersionLS: " + LittleEndian.toHexString(productVersionLS));
	out.println("  fileFlagsMask:    " + LittleEndian.toHexString(fileFlagsMask));
	out.println("  fileFlags:        " + LittleEndian.toHexString(fileFlags));
	out.println("  fileOS:           " + LittleEndian.toHexString(fileOS));
	out.println("  fileType:         " + LittleEndian.toHexString(fileType));
	out.println("  fileSubtype:      " + LittleEndian.toHexString(fileSubtype));
	out.println("  fileDateMS:       " + LittleEndian.toHexString(fileDateMS));
	out.println("  fileDateLS:       " + LittleEndian.toHexString(fileDateLS));
    }

    public Version getFileVersion() {
	return new Version ((0xFFFF0000 & fileVersionMS) >> 16, 0x0000FFFF & fileVersionMS,
			    (0xFFFF0000 & fileVersionLS) >> 16, 0x0000FFFF & fileVersionLS);
    }

    public Version getProductVersion() {
	return new Version ((0xFFFF0000 & productVersionMS) >> 16, 0x0000FFFF & productVersionMS,
			    (0xFFFF0000 & productVersionLS) >> 16, 0x0000FFFF & productVersionLS);
    }

    // Private

    void loadFromBuffer() {
	signature		= LittleEndian.getUInt(buff, 0);
	strucVersion		= LittleEndian.getUInt(buff, 4);
	fileVersionMS		= LittleEndian.getUInt(buff, 8);
	fileVersionLS		= LittleEndian.getUInt(buff, 12);
	productVersionMS	= LittleEndian.getUInt(buff, 16);
	productVersionLS	= LittleEndian.getUInt(buff, 20);
	fileFlagsMask		= LittleEndian.getUInt(buff, 24);
	fileFlags		= LittleEndian.getUInt(buff, 28);
	fileOS			= LittleEndian.getUInt(buff, 32);
	fileType		= LittleEndian.getUInt(buff, 36);
	fileSubtype		= LittleEndian.getUInt(buff, 40);
	fileDateMS		= LittleEndian.getUInt(buff, 44);
	fileDateLS		= LittleEndian.getUInt(buff, 48);
    }
}
