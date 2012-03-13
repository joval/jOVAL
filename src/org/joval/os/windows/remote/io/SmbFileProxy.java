// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import jcifs.smb.ACE;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbRandomAccessFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.windows.identity.IACE;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.FileAccessor;
import org.joval.io.fs.FileInfo;
import org.joval.os.windows.remote.identity.SmbACE;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * An IFile wrapper for an SmbFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SmbFileProxy extends CacheFile {
    private SmbFile smbFile;
    private IACE[] aces = null;

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
