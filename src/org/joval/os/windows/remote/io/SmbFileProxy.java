// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.IOException;

import jcifs.smb.SmbFile;

import org.joval.io.fs.CacheFile;
import org.joval.io.fs.FileAccessor;

/**
 * An IFile wrapper for an SmbFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SmbFileProxy extends CacheFile {
    SmbFileProxy(SmbFilesystem fs, SmbFile smbFile, String path) {
	super(fs, path);
	accessor = new SmbAccessor(fs, smbFile);
    }

    public String toString() {
	if (accessor == null) {
	    return getPath();
	} else {
	    return accessor.toString();
	}
    }

    public FileAccessor getAccessor() {
	return accessor;
    }

    public String getCanonicalPath() throws IOException {
	return accessor.getCanonicalPath();
    }
}
