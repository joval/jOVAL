// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.util.Hashtable;

/**
 * Interface to Windows license data.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ILicenseData {
    public interface IEntry {
	int TYPE_SZ	= 1;
	int TYPE_BINARY	= 2;
	int TYPE_DWORD	= 4;

	int length();

	int getType();

	String getName();

	String toString();
    }

    public interface IBinaryEntry extends IEntry {
	byte[] getData();
    }

    public interface IDwordEntry extends IEntry {
	int getData();
    }

    public interface IStringEntry extends IEntry {
	String getData();
    }

    Hashtable<String, IEntry> getEntries();
}
