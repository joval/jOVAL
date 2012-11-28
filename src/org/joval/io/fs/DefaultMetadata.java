// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.IOException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.ISearchable;
import org.joval.intf.system.ISession;
import org.joval.intf.system.IEnvironment;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * A DefaultMetadata object contains information about a file. It can be constructed from an IAccessor, or directly
 * from data gathered through other means (i.e., cached data). Subclasses are used to store platform-specific file
 * information.  Sublcasses should implement whatever extension of IFileEx is appropriate.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DefaultMetadata implements IFileMetadata, IFileEx {
    protected String path, linkPath, canonicalPath;
    protected long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, length=-1L;
    protected Type type = null;

    protected DefaultMetadata() {}

    public DefaultMetadata(Type type, String path, String linkPath, String canonicalPath, IAccessor a)
		throws IOException {

	this.type = type;
	this.path = path;
	this.linkPath = linkPath;
	this.canonicalPath = canonicalPath;
	ctime = a.getCtime();
	mtime = a.getMtime();
	atime = a.getAtime();
	length = a.getLength();
    }

    public DefaultMetadata(Type type, String path, String linkPath, String canonicalPath,
		long ctime, long mtime, long atime, long length) {

	this.path = path;
	this.linkPath = linkPath;
	this.canonicalPath = canonicalPath;
	this.ctime = ctime;
	this.mtime = mtime;
	this.atime = atime;
	this.type = type;
	this.length = length;
    }

    // Implement IFileMetadata

    public long createTime() {
	return ctime;
    }

    public long lastModified() {
	return mtime;
    }

    public long accessTime() {
	return atime;
    }

    public long length() {
	return length;
    }

    public Type getType() {
	return type;
    }

    public String getLinkPath() throws IllegalStateException, IOException {
	if (type == Type.LINK) {
	    return linkPath;
	} else {
	    throw new IllegalStateException(type.toString());
	}
    }

    public String getPath() {
	return path;
    }

    public String getCanonicalPath() {
	return canonicalPath;
    }

    public IFileEx getExtended() {
	return this;
    }
}

