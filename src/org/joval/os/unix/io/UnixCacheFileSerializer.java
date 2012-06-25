// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.io.fs.CacheFileSerializer;

/**
 * JDBM object serializer for Unix CacheFile instances.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class UnixCacheFileSerializer extends CacheFileSerializer {
    private transient UnixFilesystem fs;

    UnixCacheFileSerializer(UnixFilesystem fs) {
	super(fs);
	this.fs = fs;
    }

    @Override
    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	return new UnixFile(fs, new UnixFileInfo(in), path);
    }
}
