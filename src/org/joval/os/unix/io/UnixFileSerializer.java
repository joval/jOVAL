// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.io.fs.AbstractFilesystem;

/**
 * JDBM Serilizer implementation for Unix IFiles
 */
public class UnixFileSerializer implements Serializer<IFile>, Serializable {
    static final int SER_FILE = 0;
    static final int SER_DIRECTORY = 1;
    static final int SER_LINK = 2;

    private Integer instanceKey;
    private transient AbstractFilesystem fs;

    /**
     * The serializer relies on an active IFilesystem, which cannot be serialized, so we serialize the hashcode
     * of the IFilesystem, and maintain a static Map in the parent class. 
     */
    public UnixFileSerializer(Integer instanceKey) {
	this.instanceKey = instanceKey;
    }

    // Implement Serializer<IFile>

    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	String linkTarget = null;
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
	    linkTarget = in.readUTF();
	    break;
	}
	long len = in.readLong();
	char unixType = in.readChar();
	String permissions = in.readUTF();
	int uid = in.readInt();
	int gid = in.readInt();
	boolean hasExtendedAcl = in.readBoolean();
	Properties extended = null;
	if (in.readBoolean()) {
	    extended = new Properties();
	    int propertyCount = in.readInt();
	    for (int i=0; i < propertyCount; i++) {
		extended.setProperty(in.readUTF(), in.readUTF());
	    }
	}
	UnixFileInfo info = new UnixFileInfo(type, path, linkTarget, ctime, mtime, atime, len, unixType,
					     permissions, uid, gid, hasExtendedAcl, extended);
	if (fs == null) {
	    fs = AbstractFilesystem.instances.get(instanceKey);
	}
	return fs.createFileFromInfo(info);
    }

    public void serialize(DataOutput out, IFile f) throws IOException {
	out.writeUTF(f.getPath());
	out.writeLong(f.createTime());
	out.writeLong(f.lastModified());
	out.writeLong(f.accessTime());
	IUnixFileInfo info = (IUnixFileInfo)f.getExtended();
	if (f.isLink()) {
	    out.writeInt(SER_LINK);
	    String s = f.getLinkPath();
	    out.writeUTF(s == null ? "" : s);
	} else if (f.isDirectory()) {
	    out.writeInt(SER_DIRECTORY);
	} else {
	    out.writeInt(SER_FILE);
	}
	out.writeLong(f.length());

	String unixType = info.getUnixFileType();
	if (IUnixFileInfo.FILE_TYPE_DIR.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.DIR_TYPE);
	} else if (IUnixFileInfo.FILE_TYPE_FIFO.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.FIFO_TYPE);
	} else if (IUnixFileInfo.FILE_TYPE_LINK.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.LINK_TYPE);
	} else if (IUnixFileInfo.FILE_TYPE_BLOCK.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.BLOCK_TYPE);
	} else if (IUnixFileInfo.FILE_TYPE_CHAR.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.CHAR_TYPE);
	} else if (IUnixFileInfo.FILE_TYPE_SOCK.equals(unixType)) {
	    out.writeChar(IUnixFileInfo.SOCK_TYPE);
	} else {
	    out.writeChar(IUnixFileInfo.FILE_TYPE);
	}

	out.writeUTF(info.getPermissions());
	out.writeInt(info.getUserId());
	out.writeInt(info.getGroupId());
	out.writeBoolean(info.hasExtendedAcl());
	String[] extendedKeys = info.getExtendedKeys();
	if (extendedKeys == null) {
	    out.writeBoolean(false);
	} else {
	    out.writeBoolean(true);
	    out.writeInt(extendedKeys.length);
	    for (int i=0; i < extendedKeys.length; i++) {
		out.writeUTF(extendedKeys[i]);
		out.writeUTF(info.getExtendedData(extendedKeys[i]));
	    }
	}
    }
}
