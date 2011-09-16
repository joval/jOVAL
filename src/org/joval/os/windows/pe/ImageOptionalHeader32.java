// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.IOException;
import java.io.PrintStream;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.StreamTool;
import org.joval.io.LittleEndian;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * See http://msdn.microsoft.com/en-us/library/ms680339%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageOptionalHeader32 extends ImageOptionalHeader {
    static final int BUFFER_SIZE = 222;

    //
    // Unique to 32-bit
    //
    int baseOfData;
    int imageBase;
    int reserved1;

    public ImageOptionalHeader32(IRandomAccess ra) throws IOException {
	buff = new byte[BUFFER_SIZE];
	ra.readFully(buff);
	loadFromBuffer();
    }

    public int getBufferSize() {
	return BUFFER_SIZE;
    }

    public void debugPrint(PrintStream out) {
	out.println("RAW Buffer for IMAGE_OPTIONAL_HEADER32:");
	StreamTool.hexDump(buff, out);
	out.println("IMAGE_OPTIONAL_HEADER32:");
	out.println("  magic: " + LittleEndian.toHexString(magic));
	out.println("  majorLinkerVersion: " + LittleEndian.toHexString(majorLinkerVersion));
	out.println("  minorLinkerVersion: " + LittleEndian.toHexString(minorLinkerVersion));
	out.println("  sizeOfInitializedData: " + LittleEndian.toHexString(sizeOfInitializedData));
	out.println("  sizeOfUninitializedData: " + LittleEndian.toHexString(sizeOfUninitializedData));
	out.println("  addressOfEntryPoint: " + LittleEndian.toHexString(addressOfEntryPoint));
	out.println("  baseOfCode: " + LittleEndian.toHexString(baseOfCode));
	out.println("  baseOfData: " + LittleEndian.toHexString(baseOfData));
	out.println("  imageBase: " + LittleEndian.toHexString(imageBase));
	out.println("  sectionAlignment: " + LittleEndian.toHexString(sectionAlignment));
	out.println("  fileAlignment: " + LittleEndian.toHexString(fileAlignment));
	out.println("  majorOperatingSystemVersion: " + LittleEndian.toHexString(majorOperatingSystemVersion));
	out.println("  minorOperatingSystemVersion: " + LittleEndian.toHexString(minorOperatingSystemVersion));
	out.println("  majorImageVersion: " + LittleEndian.toHexString(majorImageVersion));
	out.println("  minorImageVersion: " + LittleEndian.toHexString(minorImageVersion));
	out.println("  majorSubsystemVersion: " + LittleEndian.toHexString(majorSubsystemVersion));
	out.println("  minorSubsystemVersion: " + LittleEndian.toHexString(minorSubsystemVersion));
	out.println("  reserved1: " + LittleEndian.toHexString(reserved1));
	out.println("  sizeOfImage: " + LittleEndian.toHexString(sizeOfImage));
	out.println("  sizeOfHeaders: " + LittleEndian.toHexString(sizeOfHeaders));
	out.println("  checkSum: " + LittleEndian.toHexString(checkSum));
	out.println("  subsystem: " + LittleEndian.toHexString(subsystem));
	out.println("  dllCharacteristics: " + LittleEndian.toHexString(dllCharacteristics));
	out.println("  sizeOfStackReserve: " + LittleEndian.toHexString(sizeOfStackReserve));
	out.println("  sizeOfStackCommit: " + LittleEndian.toHexString(sizeOfStackCommit));
	out.println("  sizeOfHeapReserve: " + LittleEndian.toHexString(sizeOfHeapReserve));
	out.println("  sizeOfHeapCommit: " + LittleEndian.toHexString(sizeOfHeapCommit));
	out.println("  loaderFlags: " + LittleEndian.toHexString(loaderFlags));
	out.println("  numberOfRvaAndSizes: " + LittleEndian.toHexString(numberOfRvaAndSizes));
	for (int i=0; i < dataDirectories.length; i++) {
	    dataDirectories[i].debugPrint(out, i);
	}
    }

    // Private

    private void loadFromBuffer() {
	if (buff.length != BUFFER_SIZE) {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_WINPE_BUFFERLEN, buff.length);
	    throw new IllegalArgumentException(s);
	}

	magic = IMAGE_NT_OPTIONAL_HDR32_MAGIC;

	majorLinkerVersion		= (byte)(buff[0] & 0xFF);
	minorLinkerVersion		= (byte)(buff[1] & 0xFF);
	sizeOfCode			= LittleEndian.getUInt(buff, 2);
	sizeOfInitializedData		= LittleEndian.getUInt(buff, 6);
	sizeOfUninitializedData		= LittleEndian.getUInt(buff, 10);
	addressOfEntryPoint 		= LittleEndian.getUInt(buff, 14);
	baseOfCode			= LittleEndian.getUInt(buff, 18);

	baseOfData			= LittleEndian.getUInt(buff, 22);
	imageBase			= LittleEndian.getUInt(buff, 26);
	sectionAlignment		= LittleEndian.getUInt(buff, 30);
	fileAlignment			= LittleEndian.getUInt(buff, 34);
	majorOperatingSystemVersion	= LittleEndian.getUShort(buff, 38);
	minorOperatingSystemVersion	= LittleEndian.getUShort(buff, 40);
	majorImageVersion		= LittleEndian.getUShort(buff, 42);
	minorImageVersion		= LittleEndian.getUShort(buff, 44);
	majorSubsystemVersion		= LittleEndian.getUShort(buff, 46);
	minorSubsystemVersion		= LittleEndian.getUShort(buff, 48);
	reserved1			= LittleEndian.getUInt(buff, 50);
	sizeOfImage			= LittleEndian.getUInt(buff, 54);
	sizeOfHeaders			= LittleEndian.getUInt(buff, 58);
	checkSum			= LittleEndian.getUInt(buff, 62);
	subsystem			= LittleEndian.getUShort(buff, 66);
	dllCharacteristics		= LittleEndian.getUShort(buff, 68);
	sizeOfStackReserve		= LittleEndian.getUInt(buff, 70);
	sizeOfStackCommit		= LittleEndian.getUInt(buff, 74);
	sizeOfHeapReserve		= LittleEndian.getUInt(buff, 78);
	sizeOfHeapCommit		= LittleEndian.getUInt(buff, 82);
	loaderFlags			= LittleEndian.getUInt(buff, 86);
	numberOfRvaAndSizes		= LittleEndian.getUInt(buff, 90);

	loadDataDirectories(94);
    }
}
