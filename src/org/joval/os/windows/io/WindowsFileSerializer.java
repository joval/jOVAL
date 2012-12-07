// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.io.fs.AbstractFilesystem;

/**
 * JDBM Serilizer implementation for Windows IFiles
 */
public class WindowsFileSerializer implements Serializer<IFile>, Serializable {
    static final int SER_FILE = 0;
    static final int SER_DIRECTORY = 1;
    static final int SER_LINK = 2;

    private Integer instanceKey;
    private transient AbstractFilesystem fs;

    /**
     * The serializer relies on an active IFilesystem, which cannot be serialized, so we serialize the hashcode
     * of the IFilesystem, and maintain a static Map in the parent class. 
     */
    public WindowsFileSerializer(Integer instanceKey) {
	this.instanceKey = instanceKey;
    }

    // Implement Serializer<IFile>

    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	String canonicalPath = in.readUTF();
	long ctime = in.readLong();
	long mtime = in.readLong();
	long atime = in.readLong();
	IFileMetadata.Type type = IFileMetadata.Type.FILE;
	switch(in.readInt()) {
	  case SER_DIRECTORY:
	    type = IFileMetadata.Type.DIRECTORY;
	    break;
	  case SER_LINK:
	    type = IFileMetadata.Type.LINK;
	    break;
	}
	long len = in.readLong();
	int winType = in.readInt();
	WindowsFileInfo info = new WindowsFileInfo(type, path, canonicalPath, ctime, mtime, atime, len, winType);
	if (fs == null) {
	    fs = AbstractFilesystem.instances.get(instanceKey);
	}
	return fs.createFileFromInfo(info);
    }

    public void serialize(DataOutput out, IFile f) throws IOException {
	out.writeUTF(f.getPath());
	out.writeUTF(f.getCanonicalPath());
	out.writeLong(f.createTime());
	out.writeLong(f.lastModified());
	out.writeLong(f.accessTime());
	if (f.isLink()) {
	    out.writeInt(SER_LINK);
	} else if (f.isDirectory()) {
	    out.writeInt(SER_DIRECTORY);
	} else {
	    out.writeInt(SER_FILE);
	}
	out.writeLong(f.length());
	IWindowsFileInfo info = (IWindowsFileInfo)f.getExtended();
	out.writeInt(info.getWindowsFileType());
    }
}
