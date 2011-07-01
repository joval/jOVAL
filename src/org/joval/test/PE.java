// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.ISession;
import org.joval.util.Version;
import org.joval.windows.pe.ImageDOSHeader;
import org.joval.windows.pe.ImageDataDirectory;
import org.joval.windows.pe.ImageNTHeaders;
import org.joval.windows.pe.ImageSectionHeader;
import org.joval.windows.pe.resource.ImageResourceDataEntry;
import org.joval.windows.pe.resource.ImageResourceDirectory;
import org.joval.windows.pe.resource.ImageResourceDirectoryEntry;
import org.joval.windows.pe.resource.Types;
import org.joval.windows.pe.resource.version.VsVersionInfo;
import org.joval.windows.pe.resource.version.VsFixedFileInfo;

public class PE {
    ISession session;

    public PE(ISession session) {
	this.session = session;
    }

    public void test(String path) {
	System.out.println("Scanning " + path);
	IFilesystem fs = session.getFilesystem();
	IRandomAccess ra = null;
	try {
	    ra =  fs.getRandomAccess(path, "r");
	    ImageDOSHeader dh = new ImageDOSHeader(ra);
	    dh.debugPrint(System.out);
	    ra.seek((long)dh.getELFHeaderRVA());
	    ImageNTHeaders nh = new ImageNTHeaders(ra);
	    nh.debugPrint(System.out);
	    long rba = nh.getResourceBaseAddress(ImageDataDirectory.RESOURCE_TABLE);
	    if (rba == 0) {
		System.out.println("There is no resource section!");
		return;
	    }
	    ra.seek(rba);
	    ImageResourceDirectory root = new ImageResourceDirectory(ra);
	    System.out.println("Dir: Resources Root");
	    traverse(root, 1, nh, ra, rba, nh.getImageDirEntryRVA(ImageDataDirectory.RESOURCE_TABLE));
	    ImageResourceDataEntry vde = getDirectoryEntry(Types.RT_VERSION, root, 1, ra, rba).getDataEntry(ra, rba);
	    ra.seek(vde.getDataAddress(rba, nh.getImageDirEntryRVA(ImageDataDirectory.RESOURCE_TABLE)));
	    VsVersionInfo vi = new VsVersionInfo(ra);
	    vi.debugPrint(System.out);
	    VsFixedFileInfo vffi = vi.getValue();
	    System.out.println("PRODUCT VERSION STRING: " + vffi.getProductVersion().toString());
	    System.out.println("FILE VERSION STRING: " + vffi.getFileVersion().toString());
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {
		if (ra != null) {
		    ra.close();
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    void traverse(ImageResourceDirectory dir, int level, ImageNTHeaders nh, IRandomAccess ra, long rba, long rva) throws IOException {
	StringBuffer padding = new StringBuffer();
	for (int i=0; i < level; i++) {
	    padding.append("  ");
	}

	ImageResourceDirectoryEntry[] entries = dir.getChildEntries();
	for (int i=0; i < entries.length; i++) {
	    ImageResourceDirectoryEntry entry = entries[i];
	    String name = entry.getName(ra, rba);
	    if (entry.isDir()) {
		int type = entry.getType();
		if (level == 1 && type >= 0 && type < Types.NAMES.length) {
		    System.out.println(padding.toString() + "Dir: " + Types.NAMES[type]);
		} else {
		    System.out.println(padding.toString() + "Dir: " + name);
		}
		ra.seek(rba + entry.getOffset());
		traverse(new ImageResourceDirectory(ra), level+1, nh, ra, rba, rva);
	    } else {
		ImageResourceDataEntry de = entry.getDataEntry(ra, rba);
		long addr = de.getDataAddress(rba, rva);
		System.out.println(padding.toString() + "Entry: " + Long.toHexString(addr));
	    }
	}
    }

    /**
     * Retrieves the first leaf node associated with the specified type.
     *
     * @param type the constant from the Types class indicating the resource to retrieve.
     * @param ra the PE file
     * @param rba the position in the PE file to the start of the RESOURCE image directory.
     */
    ImageResourceDirectoryEntry getDirectoryEntry(int type, ImageResourceDirectory dir, int level, IRandomAccess ra, long rba) throws IOException {
	ImageResourceDirectoryEntry[] entries = dir.getChildEntries();
	for (int i=0; i < entries.length; i++) {
	    ImageResourceDirectoryEntry entry = entries[i];
	    if (entry.isDir()) {
		if (level == 1 && type != entries[i].getType()) {
		    continue;
		} else {
		    ra.seek(rba + entry.getOffset());
		    ImageResourceDirectory subdir = new ImageResourceDirectory(ra);
		    return getDirectoryEntry(type, subdir, level+1, ra, rba);
		}
	    } else {
		return entry;
	    }
	}
	return null;
    }
}
