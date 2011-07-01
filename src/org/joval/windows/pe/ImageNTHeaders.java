// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe;

import java.io.IOException;
import java.io.PrintStream;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joval.intf.io.IRandomAccess;
import org.joval.io.LittleEndian;
import org.joval.util.JOVALSystem;

/**
 * IMAGE_NT_HEADERS structure.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ImageNTHeaders {
    int  signature; // DWORD signature

    ImageFileHeader fileHeader;
    ImageOptionalHeader optionalHeader;
    ImageSectionHeader[] sections;

    public ImageNTHeaders(IRandomAccess ra) throws IOException {
	signature = LittleEndian.readUInt(ra);
	fileHeader = new ImageFileHeader(ra);
	short magic = LittleEndian.readUShort(ra);
	switch (magic) {
	  case ImageOptionalHeader.IMAGE_NT_OPTIONAL_HDR32_MAGIC:
	    optionalHeader = new ImageOptionalHeader32(ra);
	    break;

	  case ImageOptionalHeader.IMAGE_NT_OPTIONAL_HDR64_MAGIC:
	  default:
	    optionalHeader = new ImageOptionalHeader64(ra);
	    break;
	}
	int numSections = fileHeader.numberOfSections;
	sections = new ImageSectionHeader[numSections];
	for (int i=0; i < numSections; i++) {
	    sections[i] = new ImageSectionHeader(ra);
	}
    }

    /**
     * Return the size of all the NT headers.
     */
    public int getNTHeaderSize() {
	return  4 +	// ImageNTHeader signature
		ImageFileHeader.BUFFER_SIZE +
		2 +	// ImageOptionalHeader magic
		optionalHeader.getBufferSize() +
		(sections.length * ImageSectionHeader.BUFFER_SIZE);
    }

    public void debugPrint(PrintStream out) {
	out.println("NT Header:");
	out.println("  signature: " + LittleEndian.toHexString(signature));
	fileHeader.debugPrint(out);
	optionalHeader.debugPrint(out);
	for (int i=0; i < sections.length; i++) {
	    sections[i].debugPrint(out);
	}
    }

    public ImageFileHeader getImageFileHeader() {
	return fileHeader;
    }

    public ImageOptionalHeader getImageOptionalHeader() {
	return optionalHeader;
    }

    public ImageSectionHeader getImageSectionHeader(int i) {
	if (i < 0 || i > sections.length) {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_WINPE_ILLEGALSECTION",
								      new Integer(sections.length), new Integer(i)));
	}
	return sections[i];
    }

    /**
     * Return the position in the PE file of the specified directory.
     *
     * @arg ide A number between 1 and 15, corresponding to the desired ImageDataDirectory._TABLE constant.
     */
    public long getResourceBaseAddress(int ide) {
	return (long)getOffsetFromRva(getImageDirEntryRVA(ide));
    }

    /**
     * Return the RVA of the image dir corresponding to the table.
     *
     * @param ide int from from org.joval.windows.pe.ImageDataDirectory.[X]_TABLE constants.
     */
    public int getImageDirEntryRVA(int ide) {
	return optionalHeader.dataDirectories[ide].virtualAddress;
    }

    // Private

    int getOffsetFromRva(int rva) {
	ImageSectionHeader sh = getEnclosingSectionHeader(rva);
	if (sh == null) {
	    return 0;
	}
	int delta = sh.virtualAddress - sh.pointerToRawData;
	return rva - delta;
    }

    ImageSectionHeader getEnclosingSectionHeader(int rva) {
	for (int i=0; i < sections.length; i++) {
	    int size = sections[i].virtualSize;
	    if (0 == size) {
		size = sections[i].sizeOfRawData;
	    }
	    if ((rva >= sections[i].virtualAddress) && (rva < (sections[i].virtualAddress + size))) {
		return sections[i];
	    }
	}
	return null;
    }
}
