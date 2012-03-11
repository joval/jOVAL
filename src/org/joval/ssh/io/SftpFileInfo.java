// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.SftpATTRS;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

import org.joval.os.unix.io.UnixFileInfo;
import org.joval.io.fs.FileInfo;

/**
 * UnixFileInfo based on an SftpFile.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class SftpFileInfo extends UnixFileInfo {
    SftpFileInfo(SftpATTRS attrs, String permissions, String path, ChannelSftp cs) throws SftpException {
	atime = attrs.getAccessTime() * 1000L;
	ctime = FileInfo.UNKNOWN_TIME;
	mtime = attrs.getModifiedTime() * 1000L;

	type = FileInfo.Type.FILE;
	unixType = permissions.charAt(0);
	switch(unixType) {
	  case DIR_TYPE:
	    type = FileInfo.Type.DIRECTORY;
	    break;
	  case LINK_TYPE:
	    type = FileInfo.Type.LINK;
	    canonicalPath = cs.realpath(path);
	    break;
	}

	uid = attrs.getUId();
	gid = attrs.getGId();

	length = attrs.getSize();

	this.permissions = permissions.substring(1);

	String[] exAttrs = attrs.getExtended();
	hasExtendedAcl = exAttrs != null && exAttrs.length > 0;

	this.path = path;
    }
}
