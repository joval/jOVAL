// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.PrintStream;

/**
 * See http://msdn.microsoft.com/en-us/library/ms680339%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class ImageOptionalHeader {
    public static final short IMAGE_NT_OPTIONAL_HDR32_MAGIC = 0x10b;
    public static final short IMAGE_NT_OPTIONAL_HDR64_MAGIC = 0x20b;

    //
    // Common to 32- and 64-bit
    //
    short magic;
    byte  majorLinkerVersion;
    byte  minorLinkerVersion;
    int   sizeOfCode;
    int   sizeOfInitializedData;
    int   sizeOfUninitializedData;
    int   addressOfEntryPoint;
    int   baseOfCode;
    int   sectionAlignment;
    int   fileAlignment;
    short majorOperatingSystemVersion;
    short minorOperatingSystemVersion;
    short majorImageVersion;
    short minorImageVersion;
    short majorSubsystemVersion;
    short minorSubsystemVersion;
    int   sizeOfImage;
    int   sizeOfHeaders;
    int   checkSum;
    short subsystem;
    short dllCharacteristics;
    int   sizeOfStackReserve;
    int   sizeOfStackCommit;
    int   sizeOfHeapReserve;
    int   sizeOfHeapCommit;
    int   loaderFlags;
    int   numberOfRvaAndSizes;
    ImageDataDirectory[] dataDirectories;

    byte[] buff;

    public abstract void debugPrint(PrintStream out);

    public abstract int getBufferSize();

    public int getChecksum() {
	return checkSum;
    }

    void loadDataDirectories(int offset) {
	dataDirectories = new ImageDataDirectory[ImageDataDirectory.IMAGE_NUMBEROF_DIRECTORY_ENTRIES];
	for (int i=0; i < dataDirectories.length; i++) {
	    dataDirectories[i] = new ImageDataDirectory(buff, offset);
	    offset += ImageDataDirectory.BUFFER_SIZE;
	}
    }
}
