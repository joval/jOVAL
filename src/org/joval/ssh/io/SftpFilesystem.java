// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.Session;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.exception.JSchException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.system.IEnvironment;
import org.joval.io.PerishableReader;
import org.joval.util.tree.CachingTree;
import org.joval.util.tree.Tree;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * An implementation of the IFilesystem interface for SSH-connected sessions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SftpFilesystem extends CachingTree implements IFilesystem {
    final static char DELIM_CH = '/';
    final static String DELIM_STR = "/";

    private Session jschSession;
    private IBaseSession session;
    private IEnvironment env;
    private IProperty props;
    private boolean autoExpand = true;
    private boolean preloaded = false;
    private long S, M, L, XL;

    ChannelSftp cs;

    public SftpFilesystem(Session jschSession, IBaseSession session, IEnvironment env) {
	super();
	this.jschSession = jschSession;
	this.session = session;
	this.env = env;
	props = session.getProperties();
	setLogger(session.getLogger());

	S = session.getTimeout(IUnixSession.Timeout.S);
	M = session.getTimeout(IUnixSession.Timeout.M);
	L = session.getTimeout(IUnixSession.Timeout.L);
	XL= session.getTimeout(IUnixSession.Timeout.XL);
    }

    public void setJschSession(Session jschSession) {
	this.jschSession = jschSession;
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
	if (!props.getBooleanProperty(PROP_PRELOAD_REMOTE)) {
	    return false;
	} else if (preloaded) {
	    return true;
	}

	ITreeBuilder tree = cache.getTreeBuilder("");
	if (tree == null) {
	    tree = new Tree("", DELIM_STR);
	    cache.addTree(tree);
	}

	try {
	    String command = null;
	    switch(session.getType()) {
	      case UNIX:
		command = getFindCommand((IUnixSession)session);
		break;

	      default:
		return false;
	    }

	    //
	    // Get either a reader for a remote file containing the results of a find command, or a reader
	    // attached to a running find command, depending on the configured method.
	    //
	    IProcess p = null;
	    ErrorReader er = null;
	    IReader reader = null;
	    if (VAL_FILE_METHOD.equals(props.getProperty(PROP_PRELOAD_METHOD))) {
		reader = PerishableReader.newInstance(getFindCache(command).getInputStream(), S);
	    } else {
		p = session.createProcess(command);
		p.start();
		reader = PerishableReader.newInstance(p.getInputStream(), S);
		er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
		er.start();
	    }

	    //
	    // Add entries to the cache, but no more than the maximum configured number
	    //
	    int entries = 0;
	    int maxEntries = props.getIntProperty(PROP_PRELOAD_MAXENTRIES);
	    String line = null;
	    while((line = reader.readLine()) != null) {
		if (entries++ < maxEntries) {
		    String path = line;
		    if (!path.equals(getDelimiter())) { // skip the root node
			INode node = tree.getRoot();
			try {
			    while ((path = trimToken(path, getDelimiter())) != null) {
				node = node.getChild(getToken(path, getDelimiter()));
			    }
			} catch (UnsupportedOperationException e) {
			    do {
				node = tree.makeNode(node, getToken(path, getDelimiter()));
			    } while ((path = trimToken(path, getDelimiter())) != null);
			} catch (NoSuchElementException e) {
			    do {
				node = tree.makeNode(node, getToken(path, getDelimiter()));
			    } while ((path = trimToken(path, getDelimiter())) != null);
			}
		    }
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
		    break;
		}
	    }

	    //
	    // Clean-up
	    //
	    reader.close();
	    if (p != null) {
		p.waitFor(0);
	    }
	    if (er != null) {
		er.join();
	    }
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.ERROR_PRELOAD);
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    session.getLogger().warn(JOVALMsg.ERROR_IO, path, e.getMessage());
	    session.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return null;
	}
    }

    // Implement IFilesystem

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
	    session.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public void disconnect() {
	try {
	    if (cs != null && cs.isConnected()) {
		cs.disconnect();
		session.disconnect();
	    }
	} catch (Throwable e) {
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
	if (!connect()) {
	    throw new IOException(JOVALSystem.getMessage(JOVALMsg.ERROR_SSH_DISCONNECTED));
	}
	if (env != null && autoExpand) {
	    path = env.expand(path);
	}
	path = getRedirect(path);
	if (path.length() > 0 && path.charAt(0) == DELIM_CH) {
	    return new SftpFile(this, path);
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, path));
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

    // Internal

    ChannelSftp getCS() {
	return cs;
    }

    // Private

    private String getFindCommand(IUnixSession us) throws IOException {
	StringBuffer command = new StringBuffer("find -L");
	String skip = props.getProperty(PROP_PRELOAD_SKIP);
	if (skip == null) {
	    session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD, "/");
	    command.append(" /");
	} else {
	    Collection<String> forbidden = StringTools.toList(StringTools.tokenize(skip, ":", true));
	    IFile[] roots = getFile(DELIM_STR).listFiles();
	    Collection<String> filtered = new Vector<String>();
	    for (int i=0; i < roots.length; i++) {
		if (forbidden.contains(roots[i].getName())) {
		    session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, roots[i]);
		} else {
		    session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD, roots[i]);
		    filtered.add(roots[i].getPath());
		}
	    }
	    for (String s : filtered) {
		command.append(" ").append(s);
	    }
	}

	//
 	// REMIND (DAS): Add logic to skip over NFS mounts for all Unix flavors.
	//
	switch(us.getFlavor()) {
	  case AIX:
	    command.append(" -fstype jfs2");
	    break;
	}
	return command.toString();
    }

    private IFile getFindCache(String command) throws Exception {
	String tempTemp = "/tmp/.jOVAL.find~";
	String tempDest = "/tmp/.jOVAL.find";
	IFile temp = getFile(tempDest);

	boolean rebuild = true;
	if (temp.exists()) {
	    long expires = temp.lastModified() + props.getLongProperty(PROP_PRELOAD_MAXAGE);
	    if (System.currentTimeMillis() >= expires) {
		temp.delete();
	    } else {
		session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD_FILE_REUSE, temp.getPath());
		rebuild = false;
	    }
	}

	if (rebuild) {
	    temp = null;
	    session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD_FILE_CREATE, tempTemp);
	    command = new StringBuffer(command).append(" > ").append(tempTemp).toString();
	    IProcess p = session.createProcess(command);
	    p.start();
	    InputStream in = p.getInputStream();
	    if (in instanceof PerishableReader) {
		// This could take a while!
		((PerishableReader)in).setTimeout(XL);
	    }
	    ErrorReader er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
	    er.start();
	    byte[] buff = new byte[1024];
	    while (in.read(buff) > 0) {} // wait...
	    if (p.isRunning()) {
		p.destroy();
	    }
	    er.join();
	    in.close();

	    SafeCLI.exec("mv " + tempTemp + " " + tempDest, session, IUnixSession.Timeout.S);
	    session.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD_FILE_CREATE, tempDest);
	}

	return getFile(tempDest);
    }

    class ErrorReader implements Runnable {
	IReader err;
	Thread t;

	ErrorReader(IReader err) {
	    err.setLogger(session.getLogger());
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
	    try {
		String line = null;
		while((line = err.readLine()) != null) {
		    session.getLogger().warn(JOVALMsg.ERROR_PRELOAD_LINE, line);
		}
	    } catch (IOException e) {
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		try {
		    err.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
