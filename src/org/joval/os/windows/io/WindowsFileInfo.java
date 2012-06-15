// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.fs.FileInfo;

/**
 * Implements extended attributes of a file on Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileInfo extends FileInfo implements IWindowsFileInfo {
    private IWindowsFileInfo provider;

    protected WindowsFileInfo(Type type, IWindowsFileInfo provider) {
	this.type = type;
	this.provider = provider;
    }

    /**
     * Create a WindowsFile with a live IFile accessor.
     */
    public WindowsFileInfo(long ctime, long mtime, long atime, Type type, long length, IWindowsFileInfo provider) {
	super(ctime, mtime, atime, type, length);
	this.provider = provider;
    }

    public WindowsFileInfo(DataInput in) throws IOException {
	super(in);
	provider = new InternalProvider(in);
    }

    public void write(DataOutput out) throws IOException {
	super.write(out);
	out.writeInt(getWindowsFileType());
	IACE[] aces = getSecurity();
	out.writeInt(aces.length);
	for (int i=0; i < aces.length; i++) {
	    out.writeInt(aces[i].getAccessMask());
	    out.writeUTF(aces[i].getSid());
	}
    }

    // Implement IWindowsFileInfo

    /**
     * Returns one of the FILE_TYPE_ constants.
     */
    public final int getWindowsFileType() throws IOException {
	return provider.getWindowsFileType();
    }

    public final IACE[] getSecurity() throws IOException {
	return provider.getSecurity();
    }

    // Private

    private class InternalProvider implements IWindowsFileInfo {
	private int winType;
	private IACE[] aces;

	InternalProvider(DataInput in) throws IOException {
	    winType = in.readInt();
	    int numAces = in.readInt();
	    aces = new IACE[numAces];
	    for (int i=0; i < numAces; i++) {
		aces[i] = new InternalAce(in.readInt(), in.readUTF());
	    }
	}

	public int getWindowsFileType() {
	    return winType;
	}

	public IACE[] getSecurity() {
	    return aces;
	}
    }

    private class InternalAce implements IACE {
	private int mask;
	private String sid;

	InternalAce(int mask, String sid) {
	    this.mask = mask;
	    this.sid = sid;
	}

	public int getAccessMask() {
	    return mask;
	}

	public String getSid() {
	    return sid;
	}
    }
}
