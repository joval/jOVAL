// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbRandomAccessFile;
import jcifs.smb.VolatileSmbFile;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.tree.INode;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.io.fs.CacheFilesystem;
import org.joval.os.windows.io.WOW3264FilesystemRedirector;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A simple abstraction of a server filesystem, to make it easy to retrieve SmbFile objects from a particular machine using
 * a particular set of credentials.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbFilesystem extends CacheFilesystem implements IWindowsFilesystem {
    static final String	LOCAL_DELIM_STR		= "\\";
    static final char	LOCAL_DELIM_CH		= '\\';
    static final String	SMBURL_DELIM_STR	= "/";
    static final char	SMBURL_DELIM_CH		= '/';

    private String host;
    private NtlmPasswordAuthentication auth;

    /**
     * Create an IFilesystem object for a remote host.
     *
     * @param env The host environment, used to expand variables that are passed inside of paths.  If null, autoExpand is
     *	    automatically set to false.
     */
    public SmbFilesystem(IBaseSession session, IWindowsCredential cred, IEnvironment env, IPathRedirector fsr) {
	super(session, env, fsr, LOCAL_DELIM_STR);
	host = session.getHostname();
	auth = getNtlmPasswordAuthentication(cred);
    }

    @Override
    protected IFile accessResource(String path) throws IllegalArgumentException, IOException {
	return accessResource(path, false);
    }

    // Implement IFilesystem

    @Override
    public IFile getFile(String path, int flags) throws IllegalArgumentException, IOException {
	try {
	    IFile f = null;
	    switch(flags) {
	      case IFile.NOCACHE:
	      case IFile.READWRITE:
		return accessResource(path, false);

	      case IFile.READVOLATILE:
		f = accessResource(path, true);
		// fall-thru
	      case IFile.READONLY:
		if (f == null) {
		    f = getResource(path);
		}
		if (f.exists()) {
		    return f;
		} else {
		    throw new FileNotFoundException(path);
		}

	      default:
		throw new IllegalArgumentException(Integer.toString(flags));
	    }
	} catch (Exception e) {
	    if (e instanceof IOException) {
		throw (IOException)e;
	    } else if (e instanceof IllegalArgumentException) {
		throw (IllegalArgumentException)e;
	    } else {
		throw new IOException(e);
	    }
	}
    }

    // Private

    /**
     * Return an SmbFile on the remote machine using a local filesystem path, e.g., "C:\Windows\System32\notepad.exe", or
     * more interestingly, if autoExpand is true, "%SystemRoot%\System32\notepad.exe".
     *
     * This method is responsible for implementing 64-bit file redirection.
     */
    private SmbFileProxy accessResource(String path, boolean vol) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
	String realPath = path;
	if (redirector != null) {
	    String alt = redirector.getRedirect(path);
	    if (alt != null) {
		realPath = alt;
	    }
	}
	if (isValidPath(realPath)) {
	    StringBuffer sb = new StringBuffer("smb://").append(host).append(SMBURL_DELIM_CH);
	    sb.append(realPath.charAt(0)).append('$');
	    sb.append(realPath.substring(2).replace(LOCAL_DELIM_CH, SMBURL_DELIM_CH));
	    logger.trace(JOVALMsg.STATUS_WINSMB_MAP, path, sb.toString());

	    SmbFile smbFile = null;
	    if (isDrive(realPath)) {
		sb.append(SMBURL_DELIM_CH);
		smbFile = new SmbFile(sb.toString(), auth);
	    } else if (vol) {
		smbFile = new VolatileSmbFile(sb.toString(), auth);
	    } else {
		smbFile = new SmbFile(sb.toString(), auth);
		//
		// For directories, it's REQUIRED that the URL conclude with a delimiter
		//
		try {
		    if (smbFile.isDirectory()) {
			smbFile = new SmbFile(sb.append(SMBURL_DELIM_CH).toString(), auth);
		    }
		} catch (SmbException e) {
		    // If this happens here, just proceed and potentially run into an error later on...
		}
	    }
	    return new SmbFileProxy(this, smbFile, path);
	}
	throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, path));
    }

    /**
     * Check for ASCII values between [A-Z] or [a-z].
     */
    boolean isLetter(char c) {
	return (c >= 65 && c <= 90) || (c >= 95 && c <= 122);
    }

    private NtlmPasswordAuthentication getNtlmPasswordAuthentication(IWindowsCredential cred) {
	return new NtlmPasswordAuthentication(cred.getDomain(), cred.getUsername(), cred.getPassword());
    }

    private boolean isValidPath(String s) {
	if (s.length() >= 2) {
	    return StringTools.isLetter(s.charAt(0)) && s.charAt(1) == ':';
	}
	return false;
    }

    private boolean isDrive(String s) {
	if (s.length() == 2) {
	    return isValidPath(s);
	}
	return false;
    }
}
