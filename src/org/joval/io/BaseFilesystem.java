// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.ITreeBuilder;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.util.tree.CachingTree;
import org.joval.util.tree.Tree;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * The base class for IFilesystem implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFilesystem extends CachingTree implements IFilesystem {
    protected boolean autoExpand = true;
    protected IProperty props;
    protected IBaseSession session;
    protected IEnvironment env;
    protected IPathRedirector redirector;

    protected BaseFilesystem(IBaseSession session, IEnvironment env, IPathRedirector redirector) {
	super();
	this.session = session;
	this.env = env;
	this.redirector = redirector;
	props = session.getProperties();
	setLogger(session.getLogger());
    }

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    // Implement ITree (methods abstract in CachingTree)

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
	    logger.warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    logger.debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return null;
    }

    // Implement IFilesystem

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
	if (realPath.length() > 0 && realPath.charAt(0) == File.separatorChar) {
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

    // Abstract

    public abstract boolean preload();

    // Inner classes

    protected class PreloadOverflowException extends Exception {
	public PreloadOverflowException() {
	    super();
	}
    }

    protected class FileProxy extends BaseFile {
	private File file;
	private String localName;

	public FileProxy(IFilesystem fs, File file, String localName) {
	    super(fs);
	    this.file = file;
	    if (localName.endsWith(fs.getDelimiter())) {
		this.localName = localName.substring(0, localName.lastIndexOf(fs.getDelimiter()));
	    } else {
		this.localName = localName;
	    }
	}

	public File getFile() {
	    return file;
	}

	// Implement methods left abstract in BaseFile

	public String getCanonicalPath() {
	    try {
		return file.getCanonicalPath();
	    } catch (IOException e) {
		return file.getPath();
	    }
	}

	// Implement IFile

	/**
	 * Not really supported in this implementation.
	 */
	public long accessTime() throws IOException {
	    return lastModified();
	}

	public long createTime() throws IOException {
	    return file.lastModified();
	}

	public boolean exists() throws IOException {
	    return file.exists();
	}

	public boolean mkdir() {
	    return file.mkdir();
	}

	public InputStream getInputStream() throws IOException {
	    return new FileInputStream(file);
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
	    return new FileOutputStream(file, append);
	}

	public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	    return new RandomAccessFileProxy(new RandomAccessFile(file, mode));
	}

	public boolean isDirectory() throws IOException {
	    return file.isDirectory();
	}

	public boolean isFile() throws IOException {
	    return file.isFile();
	}

	public boolean isLink() throws IOException {
	    return !file.getPath().equals(file.getCanonicalPath());
	}

	public long lastModified() throws IOException {
	    return file.lastModified();
	}

	public long length() throws IOException {
	    return file.length();
	}

	public String[] list() throws IOException {
	    String[] children = file.list();
	    if (children == null) {
		return new String[0];
	    } else {
		return children;
	    }
	}

	public IFile[] listFiles() throws IOException {
	    String[] children = file.list();
	    if (children == null) {
		return new IFile[0];
	    } else {
		IFile[] files = new IFile[children.length];
		for (int i=0; i < children.length; i++) {
		    files[i] = fs.getFile(localName + fs.getDelimiter() + children[i]);
		}
		return files;
	    }
	}

	public void delete() throws IOException {
	    file.delete();
	}

	public String getLocalName() {
	    return localName;
	}

	public String getName() {
	    return file.getName();
	}

	public String toString() {
	    return file.toString();
	}
    }

    protected class RandomAccessFileProxy implements IRandomAccess {
	private RandomAccessFile raf;

	public RandomAccessFileProxy(RandomAccessFile raf) {
	    this.raf = raf;
	}

	// Implement IRandomAccess

	public void readFully(byte[] buff) throws IOException {
	    raf.readFully(buff);
	}

	public void close() throws IOException {
	    raf.close();
	}

	public void seek(long pos) throws IOException {
	    raf.seek(pos);
	}

	public int read() throws IOException {
	    return raf.read();
	}

	public int read(byte[] buff) throws IOException {
	    return raf.read(buff);
	}

	public int read(byte[] buff, int offset, int len) throws IOException {
	    return raf.read(buff, offset, len);
	}

	public long length() throws IOException {
	    return raf.length();
	}

	public long getFilePointer() throws IOException {
	    return raf.getFilePointer();
	}
    }
}
