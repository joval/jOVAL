// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.remote.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.vngx.jsch.Session;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IPathRedirector;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.io.CachingFilesystem;
import org.joval.util.JOVALSystem;

/**
 * An implementation of the IFilesystem interface for SSH-connected sessions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SftpFilesystem extends CachingFilesystem {
    final static char DELIM_CH = '/';
    final static String DELIM_STR = "/";

    private Session session;
    private boolean autoExpand = true;
    private IEnvironment env;

    public SftpFilesystem(Session session, IEnvironment env) {
	super();
	this.env = env;
	this.session = session;
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement IPathRedirector

    public String getRedirect(String path) {
	return path;
    }

    // Implement IFilesystem

    public char getDelimChar() {
	return DELIM_CH;
    }

    public String getDelimString() {
	return DELIM_STR;
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
	path = getRedirect(path);
	if (path.charAt(0) == DELIM_CH) {
	    try {
	        ChannelSftp cs = session.openChannel(ChannelType.SFTP);
	        return new SftpFile(cs, path);
	    } catch (JSchException e) {
		throw new IOException(e);
	    }
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_FS_LOCALPATH", path));
	}
    }

    public IFile getFile(String path, boolean vol) throws IllegalArgumentException, IOException {
	return getFile(path);
    }

    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return file.getRandomAccess(mode);
    }

    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return getFile(path).getRandomAccess(mode);
    }

    public InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    public OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(false);
    }

    public OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(append);
    }
}
