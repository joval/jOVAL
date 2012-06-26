// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.ChannelType;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.Session;
import org.vngx.jsch.SftpATTRS;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.io.IReader;
import org.joval.intf.ssh.ISftpError;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.system.IEnvironment;
import org.joval.io.PerishableReader;
import org.joval.os.unix.io.UnixFileInfo;
import org.joval.os.unix.io.UnixFilesystem;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * An implementation of the IUnixFilesystem interface for SSH-connected sessions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SftpFilesystem extends UnixFilesystem {
    final static char DELIM_CH = '/';
    final static String DELIM_STR = "/";

    /**
     * Get the error code from an SftpException.
     *
     * @see org.joval.intf.ssh.ISftpError
     */
    public static int getErrorCode(SftpException e) {
	try {
	    String s = e.toString();
	    return Integer.parseInt(s.substring(0, s.indexOf(":")));
	} catch (Exception ex) {
	    return -1;
	}
    }

    private Session jschSession;
    private ChannelSftp cs;

    public SftpFilesystem(Session jschSession, IBaseSession session, IEnvironment env) {
	super(session, env);
	this.jschSession = jschSession;
    }

    public void setJschSession(Session jschSession) {
	this.jschSession = jschSession;
    }

    public boolean connect() {
	if (cs != null && cs.isConnected()) {
	    return true;
	}
	try {
	    if (session.connect()) {
		cs = jschSession.openChannel(ChannelType.SFTP);
		cs.connect();
		return true;
	    } else {
		session.getLogger().error(JOVALMsg.ERROR_SSH_DISCONNECTED);
		return false;
	    }
	} catch (JSchException e) {
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public void disconnect() {
	try {
	    if (cs != null && cs.isConnected()) {
		cs.disconnect();
		cs = null;
		session.disconnect();
	    }
	} catch (Throwable e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    @Override
    protected String getPreloadPropertyKey() {
	return PROP_PRELOAD_REMOTE;
    }

    private int attempt = 0;
    private int threshold = props.getIntProperty(PROP_PRELOAD_TRIGGER);

    @Override
    protected IFile accessResource(String path, int flags) throws IllegalArgumentException, IOException {
	if (!connect()) {
	    throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_SSH_DISCONNECTED));
	}
	if (env != null && autoExpand) {
	    path = env.expand(path);
	}
	if (redirector != null) {
	    String temp = redirector.getRedirect(path);
	    if (temp != null) {
		path = temp;
	    }
	}
	if (path.length() != 0 && path.charAt(0) == DELIM_CH) {
	    if (!preloaded) {
		if (threshold >= 0 && threshold == attempt++) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_AUTOSTART, threshold);
		    if (loadCache()) {
			return getFile(path);
		    }
		}
	    }
	    return new SftpFile(this, path);
	} else {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, path));
	}
    }

    // Internal

    UnixFileInfo getUnixFileInfo(SftpATTRS attrs, String permissions, String path) throws Exception {
	switch(us.getFlavor()) {
	  //
	  // On Linux, we need to use the IUnixFilesystemDriver to fetch SELinux attributes.
	  //
	  case LINUX:
	    return super.getUnixFileInfo(path);

	  //
	  // On other Unix platforms, we can use the SFTP-derived attribute data (much faster).
	  //
	  default:
	    return new SftpFileInfo(attrs, permissions, path, getCS());
	}
    }

    ChannelSftp getCS() throws IOException {
	if (!connect()) {
	    throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_SSH_DISCONNECTED));
	}
	return cs;
    }
}
