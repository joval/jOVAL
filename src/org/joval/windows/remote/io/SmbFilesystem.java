// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.smb.SmbRandomAccessFile;
import jcifs.smb.VolatileSmbFile;

import org.joval.identity.windows.WindowsCredential;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IPathRedirector;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.tree.CachingTree;
import org.joval.windows.WOW3264PathRedirector;

/**
 * A simple abstraction of a server filesystem, to make it easy to retrieve SmbFile objects from a particular machine using
 * a particular set of credentials.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbFilesystem extends CachingTree implements IFilesystem {
    static final String	LOCAL_DELIM_STR		= "\\";
    static final char	LOCAL_DELIM_CH		= '\\';
    static final String	SMBURL_DELIM_STR	= "/";
    static final char	SMBURL_DELIM_CH		= '/';

    private String host;
    private NtlmPasswordAuthentication auth;
    private IEnvironment env;
    private IPathRedirector redirector;
    private boolean autoExpand, redirect64;

    /**
     * Create an IFilesystem object for a remote host.
     *
     * @param env The host environment, used to expand variables that are passed inside of paths.  If null, autoExpand is
     *            automatically set to false.
     */
    public SmbFilesystem(String host, WindowsCredential cred, IEnvironment env) {
	super();
	this.host = host;
	auth = cred.getNtlmPasswordAuthentication();
	this.env = env;
	redirector = new WOW3264PathRedirector(env);
	autoExpand = true;
	redirect64 = false;
    }

    /**
     * Create a Filesystem object for a remote host.  The environment is retrieved from the host's registry, so that
     * it can be used to expand variables that are passed inside of paths.
     */
    public SmbFilesystem(String host, WindowsCredential cred) {
	this.host = host;
	auth = cred.getNtlmPasswordAuthentication();
	autoExpand = false;
    }

    /**
     * Enable/disable the automatic expanding of environment variables that appear in path names in the form %variable_name%.
     * By default this is set to true, unless a null Environment was passed to the constructor.
     */
    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    /**
     * If true, redirects calls to %SystemRoot%\System32 to %SystemRoot%\SysWOW64.
     */
    public void set64BitRedirect(boolean redirect) {
	redirect64 = redirect;
    }

    // Implement IPathRedirector

    public String getRedirect(String path) {
	if (redirect64) {
	    return redirector.getRedirect(path);
	} else {
	    return path;
	}
    }

    // Implement methods left abstract in CachingTree

    public String getDelimiter() {
	return LOCAL_DELIM_STR;
    }

    public INode lookup(String path) throws NoSuchElementException {
	try {
	    IFile f = null;
	    try {
		f = getFile(path);
	    } catch (IOException e) {
		if (!path.endsWith(getDelimiter())) {
		    f = getFile(path + getDelimiter());
		} else {
		    throw e;
		}
	    }
	    if (f.exists()) {
		return f;
	    } else {
		throw new NoSuchElementException(path);
	    }
	} catch (IOException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_IO", e.getMessage()), e);
	    return null;
	}
    }

    // Implement IFilesystem

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	return getFile(path, false);
    }

    /**
     * Return an SmbFile on the remote machine using a local filesystem path, e.g., "C:\Windows\System32\notepad.exe", or
     * more interestingly, if autoExpand is true, "%SystemRoot%\System32\notepad.exe".
     *
     * This method is responsible for implementing 64-bit file redirection.
     */
    public IFile getFile(String path, boolean vol) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
	path = getRedirect(path);
	if (path.length() > 2 && path.charAt(1) == ':') {
	    if (isLetter(path.charAt(0))) {
		StringBuffer sb = new StringBuffer("smb://").append(host).append(SMBURL_DELIM_CH);
		sb.append(path.charAt(0)).append('$').append(path.substring(2).replace(LOCAL_DELIM_CH, SMBURL_DELIM_CH));
		JOVALSystem.getLogger().log(Level.FINEST, JOVALSystem.getMessage("STATUS_WINSMB_MAP", path, sb.toString()));
		SmbFile smbFile = null;
		if (vol) {
		    smbFile = new VolatileSmbFile(sb.toString(), auth);
		} else {
		    smbFile = new SmbFile(sb.toString(), auth);
		}
		return new SmbFileProxy(this, smbFile, path);
	    }
	}
	throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_FS_LOCALPATH", path));
    }

    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	if (file instanceof SmbFileProxy) {
	    return new SmbRandomAccessProxy(new SmbRandomAccessFile(((SmbFileProxy)file).getSmbFile(), mode));
	}
	throw new IllegalArgumentException(JOVALSystem.getMessage("ERROR_INSTANCE", 
								  SmbFileProxy.class.getName(), file.getClass().getName()));
    }

    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return new SmbRandomAccessProxy(new SmbRandomAccessFile(((SmbFileProxy)getFile(path)).getSmbFile(), mode));
    }

    public InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    public OutputStream getOutputStream(String path) throws IllegalArgumentException, IOException {
	return getOutputStream(path, false);
    }

    public OutputStream getOutputStream(String path, boolean append) throws IllegalArgumentException, IOException {
	return getFile(path).getOutputStream(append);
    }

    // Private

    /**
     * Check for ASCII values between [A-Z] or [a-z].
     */
    boolean isLetter(char c) {
	return (c >= 65 && c <= 90) || (c >= 95 && c <= 122);
    }
}
