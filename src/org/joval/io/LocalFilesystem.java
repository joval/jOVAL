// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.system.IEnvironment;
import org.joval.os.windows.io.WindowsFile;
import org.joval.util.tree.CachingTree;
import org.joval.util.tree.Tree;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A pure-Java implementation of the IFilesystem interface.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LocalFilesystem extends CachingTree implements IFilesystem {
    private static boolean LINUX	= System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
    private static boolean WINDOWS	= System.getProperty("os.name").startsWith("Windows");

    private boolean autoExpand = true, preloaded = false;
    private ILoggable log;
    private IEnvironment env;
    private IPathRedirector redirector;

    public LocalFilesystem (IEnvironment env, IPathRedirector redirector, ILoggable log) {
	super();
	this.env = env;
	this.redirector = redirector;
	this.log = log;
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement methods left abstract in CachingTree

    public LocLogger getLogger() {
	return log.getLogger();
    }

    public void setLogger(LocLogger logger) {
	log.setLogger(logger);
    }

    public boolean preload() {
	if (!"true".equals(JOVALSystem.getProperty(JOVALSystem.PROP_LOCAL_FS_PRELOAD))) {
	    return false;
	} else if (preloaded) {
	    return true;
	}

	try {
	    if (WINDOWS) {
		File[] roots = File.listRoots();
		for (int i=0; i < roots.length; i++) {
		    String name = roots[i].getName();
		    ITreeBuilder tree = cache.getTreeBuilder(name);
		    if (tree == null) {
			tree = new Tree(name, getDelimiter());
			cache.addTree(tree);
		    }
		    addRecursive(tree, roots[i]);
		}
	    } else {
		File root = new File(getDelimiter());
		Collection<File> roots = new Vector<File>();
		if (LINUX) {
		    String skip = JOVALSystem.getProperty(JOVALSystem.PROP_LINUX_FS_SKIP);
		    if (skip == null) {
			roots.add(root);
		    } else {
			Collection<String> forbidden = StringTools.toList(StringTools.tokenize(skip, ":", true));
			String[] children = root.list();
			for (int i=0; i < children.length; i++) {
			    if (forbidden.contains(children[i])) {
				log.getLogger().info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, children[i]);
			    } else {
				log.getLogger().debug(JOVALMsg.STATUS_FS_PRELOAD, children[i]);
				roots.add(new File(root, children[i]));
			    }
			}
		    }
		} else {
		    roots.add(root);
		}

		ITreeBuilder tree = cache.getTreeBuilder("");
		if (tree == null) {
		    tree = new Tree("", getDelimiter());
		    cache.addTree(tree);
		}
		for (File f : roots) {
		    addRecursive(tree, f);
		}
	    }

	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    log.getLogger().warn(JOVALMsg.ERROR_PRELOAD);
	    log.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    public String getDelimiter() {
	return File.separator;
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
	    log.getLogger().warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    log.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return null;
    }

    // Implement IFilesystem

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public IFile getFile(String path) throws IllegalArgumentException, IOException {
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
	if (WINDOWS) {
	    if (realPath.length() > 2 && realPath.charAt(1) == ':') {
	        if (StringTools.isLetter(realPath.charAt(0))) {
		    return new WindowsFile(new FileProxy(this, new File(realPath), path));
	        }
	    }
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
	} else if (realPath.length() > 0 && realPath.charAt(0) == File.separatorChar) {
	    return new FileProxy(this, new File(realPath), path);
	} else {
	    throw new IllegalArgumentException(JOVALSystem.getMessage(JOVALMsg.ERROR_FS_LOCALPATH, realPath));
	}
    }

    public IFile getFile(String path, boolean vol) throws IllegalArgumentException, IOException {
	return getFile(path);
    }

    public IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(file.getLocalName(), mode));
    }

    public IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	return new RandomAccessFileProxy(new RandomAccessFile(path, mode));
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

    private void addRecursive(ITreeBuilder tree, File f) throws IOException {
	String path = f.getCanonicalPath();
	if (!path.equals(f.getPath())) {
	    log.getLogger().warn(JOVALMsg.ERROR_PRELOAD_LINE, path);
	} else if (f.isFile()) {
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
	} else if (f.isDirectory()) {
	    File[] children = f.listFiles();
	    if (children == null) {
		log.getLogger().warn(JOVALMsg.ERROR_PRELOAD_LINE, path);
	    } else {
		for (File child : children) {
		    addRecursive(tree, child);
		}
	    }
	} else {
	    log.getLogger().warn(JOVALMsg.ERROR_PRELOAD_LINE, path);
	}
    }
}
