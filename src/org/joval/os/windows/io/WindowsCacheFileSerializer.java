// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.DataInput;
import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.io.fs.CacheFileSerializer;

/**
 * JDBM object serializer for the Windows CacheFile instances.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class WindowsCacheFileSerializer extends CacheFileSerializer {
    private transient WindowsFilesystem fs;

    WindowsCacheFileSerializer(WindowsFilesystem fs) {
	super(fs);
	this.fs = fs;
    }

    @Override
    public IFile deserialize(DataInput in) throws IOException {
	String path = in.readUTF();
	return new WindowsFile(fs, new WindowsFileInfo(in), path);
    }
}
