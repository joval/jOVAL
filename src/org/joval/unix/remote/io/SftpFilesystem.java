// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.unix.remote.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.vngx.jsch.Session;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IPathRedirector;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.system.IEnvironment;
import org.joval.util.tree.CachingTree;
import org.joval.util.JOVALSystem;

/**
 * An implementation of the IFilesystem interface for SSH-connected sessions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SftpFilesystem extends CachingTree implements IFilesystem {
    final static char DELIM_CH = '/';
    final static String DELIM_STR = "/";

    private Session session;
    private IUnixSession unixSession;
    private boolean autoExpand = true;
    private boolean preloaded = false;

    ChannelSftp cs;

    public SftpFilesystem(Session session, IUnixSession unixSession) {
	super();
	this.session = session;
	this.unixSession = unixSession;
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement IPathRedirector

    public String getRedirect(String path) {
	return path;
    }

    // Implement methods left abstract in CachingTree

    public boolean preload() {
	if (preloaded) {
	    return true;
	}

	ITreeBuilder tree = cache.getTreeBuilder("");
	if (tree == null) {
	    tree = cache.makeTree("", DELIM_STR);
	}
	try {
	    IProcess p = unixSession.createProcess("find / *");
	    p.start();
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    ErrorReader er = new ErrorReader(p.getErrorStream());
	    er.start();
	    String line = null;
	    while((line = br.readLine()) != null) {
		String path = line;
		if (!path.equals(getDelimiter())) { // skip the root node
		    INode node = tree.getRoot();
		    try {
			while ((path = trimToken(path)) != null) {
			    node = node.getChild(getToken(path));
			}
		    } catch (UnsupportedOperationException e) {
			do {
			    node = tree.makeNode(node, getToken(path));
			} while ((path = trimToken(path)) != null);
		    } catch (NoSuchElementException e) {
			do {
			    node = tree.makeNode(node, getToken(path));
			} while ((path = trimToken(path)) != null);
		    }
		}
	    }
	    br.close();
	    er.join();
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_PRECACHE", e.getMessage()), e);
	    return false;
	}
    }

    public String getDelimiter() {
	return DELIM_STR;
    }

    public INode lookup(String path) throws NoSuchElementException {
	try {
	    IFile f = getFile(path);
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
	try {
	    cs = session.openChannel(ChannelType.SFTP);
	    cs.connect();
	    return true;
	} catch (JSchException e) {
	    JOVALSystem.getLogger().log(Level.SEVERE,
					JOVALSystem.getMessage("ERROR_IO", getRoot().getName(), e.getMessage()), e);
	    return false;
	}
    }

    public void disconnect() {
	try {
	    if (cs.isConnected()) {
		cs.disconnect();
	    }
	} catch (Throwable e) {
	    JOVALSystem.getLogger().log(Level.WARNING,
					JOVALSystem.getMessage("ERROR_IO", getRoot().getName(), e.getMessage()), e);
	}
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = unixSession.getEnvironment().expand(path);
	}
	path = getRedirect(path);
	if (path.length() > 0 && path.charAt(0) == DELIM_CH) {
	    try {
	        return new SftpFile(this, path);
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

    // Private

    class ErrorReader implements Runnable {
	InputStream err;
	Thread t;

	ErrorReader(InputStream err) {
	    this.err = err;
	}

	void start() {
	    t = new Thread(this);
	    t.start();
	}

	void join() throws InterruptedException {
	    t.join();
	}

	public void run() {
	    Logger log = JOVALSystem.getLogger();
	    try {
		BufferedReader br = new BufferedReader(new InputStreamReader(err));
		String line = null;
		while((line = br.readLine()) != null) {
		    log.log(Level.WARNING, JOVALSystem.getMessage("ERROR_PRECACHE", line));
		}
	    } catch (IOException e) {
		log.log(Level.WARNING, JOVALSystem.getMessage("ERROR_PRECACHE", e.getMessage()), e);
	    } finally {
		try {
		    err.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
