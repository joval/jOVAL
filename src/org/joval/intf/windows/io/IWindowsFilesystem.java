// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.io;

import org.joval.intf.io.IFilesystem;

public interface IWindowsFilesystem extends IFilesystem {
    String DELIM_STR = "\\";
    char DELIM_CH = '\\';

    /**
     * An enumeration of types, corresponding to the low-level DRIVE_ values.
     */
    enum FsType {
	UNKNOWN("unknown", DRIVE_UNKNOWN),
	REMOVABLE("removable", DRIVE_REMOVABLE),
	FIXED("fixed", DRIVE_FIXED),
	REMOTE("remote", DRIVE_REMOTE),
	CDROM("cdrom", DRIVE_CDROM),
	RAMDISK("ramdisk", DRIVE_RAMDISK);

	private String value;
	private int id;

	private FsType(String value, int id) {
	    this.value = value;
	    this.id = id;
	}

	public String value() {
	    return value;
	}

	public int id() {
	    return id;
	}

	public static FsType typeOf(String value) {
	    for (FsType fs : values()) {
		if (fs.value().equals(value)) {
		    return fs;
		}
	    }
	    return UNKNOWN;
	}

	public static FsType typeOf(int id) {
	    for (FsType fs : values()) {
		if (fs.id() == id) {
		    return fs;
		}
	    }
	    return UNKNOWN;
	}
    }

    /**
     * The drive type cannot be determined.
     */
    int DRIVE_UNKNOWN = 0;

    /**
     * The root path is invalid; for example, there is no volume mounted at the specified path.
     */
    int DRIVE_NO_ROOT_DIR = 1;

    /**
     * The drive has removable media; for example, a floppy drive, thumb drive, or flash card reader.
     */
    int DRIVE_REMOVABLE = 2;

    /**
     * The drive has fixed media; for example, a hard disk drive or flash drive.
     */
    int DRIVE_FIXED = 3;

    /**
     * The drive is a remote (network) drive.
     */
    int DRIVE_REMOTE = 4;

    /**
     * The drive is a CD-ROM drive.
     */
    int DRIVE_CDROM = 5;

    /**
     * The drive is a RAM disk.
     */
    int DRIVE_RAMDISK = 6;

    IWindowsFilesystemDriver getDriver();
}
