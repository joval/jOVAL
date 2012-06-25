// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.DataInput;
import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.io.fs.CacheFileSerializer;
import org.joval.os.windows.io.WindowsFileInfo;

/**
 * JDBM object serializer for remote Windows CacheFile instances.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SmbCacheFileSerializer extends CacheFileSerializer {
    private transient SmbFilesystem fs;

    SmbCacheFileSerializer(SmbFilesystem fs) {
	super(fs);
	this.fs = fs;
    }

    @Override
    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	WindowsFileInfo info = new WindowsFileInfo(in);
	return fs.accessResource(path, IFile.READONLY);
    }
}
