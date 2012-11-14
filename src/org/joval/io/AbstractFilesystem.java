// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
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
 * An abstract IFilesystem implementation with some convenience methods.
 *
 * All IFilesystem implementations extend this base class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractFilesystem implements IFilesystem {
    protected boolean autoExpand = true;
    protected IProperty props;
    protected ISession session;
    protected IEnvironment env;
    protected LocLogger logger;

    protected final String ESCAPED_DELIM;
    protected final String DELIM;

    protected AbstractFilesystem(ISession session, String delim) {
	ESCAPED_DELIM = Matcher.quoteReplacement(delim);
	DELIM = delim;
	this.session = session;
	logger = session.getLogger();
	props = session.getProperties();
	env = session.getEnvironment();
    }

    public abstract void dispose();

    public void setAutoExpand(boolean autoExpand) {
	this.autoExpand = autoExpand;
    }

    public IFile.Flags getDefaultFlags() {
	return IFile.Flags.READONLY;
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    /**
     * For use by the ISearchable.ISearchPlugin.
     */
    public IFile createFileFromInfo(String path, FileInfo info) {
	return new DefaultFile(path, info);
    }

    // Implement IFilesystem

    public String guessParent(Pattern p) {
	String path = p.pattern();
	if (!path.startsWith("^")) {
	    return null;
	}
	path = path.substring(1);

	int ptr = path.indexOf(ESCAPED_DELIM);
	if (ptr == -1) {
	    return path;
	}

	StringBuffer sb = new StringBuffer(path.substring(0,ptr));
	int edl = ESCAPED_DELIM.length();
	int next = ptr;
	while((next = path.indexOf(ESCAPED_DELIM, ptr + edl)) != -1) {
	    String token = path.substring(ptr + edl, next);
	    if (StringTools.containsRegex(token)) {
		return sb.toString();
	    } else {
		sb.append(DELIM).append(token);
		ptr = next;
	    }
	}
	if (sb.length() == 0) {
	    return null;
	} else {
	    return sb.toString();
	}
    }

    public String getDelimiter() {
	return DELIM;
    }

    public final IFile getFile(String path) throws IOException {
	return getFile(path, getDefaultFlags());
    }

    public final IRandomAccess getRandomAccess(IFile file, String mode) throws IllegalArgumentException, IOException {
	return file.getRandomAccess(mode);
    }

    public final IRandomAccess getRandomAccess(String path, String mode) throws IllegalArgumentException, IOException {
	IFile.Flags flags = "rw".equals(mode) ? flags = IFile.Flags.READWRITE : IFile.Flags.READONLY;
	return getFile(path, flags).getRandomAccess(mode);
    }

    public final InputStream getInputStream(String path) throws IllegalArgumentException, IOException {
	return getFile(path).getInputStream();
    }

    /**
     * File access layer implementation base class. Every IFile is backed by a FileAccessor, which is responsible for
     * interacting with the file.
     */
    public abstract class FileAccessor {
	public FileAccessor() {}
	public abstract boolean exists();
	public abstract FileInfo getInfo() throws IOException;
	public abstract long getCtime() throws IOException;
	public abstract long getMtime() throws IOException;
	public abstract long getAtime() throws IOException;
	public abstract long getLength() throws IOException;
	public abstract IRandomAccess getRandomAccess(String mode) throws IOException;
	public abstract InputStream getInputStream() throws IOException;
	public abstract OutputStream getOutputStream(boolean append) throws IOException;
	public abstract String getCanonicalPath() throws IOException;
	public abstract String[] list() throws IOException;
	public abstract boolean mkdir();
	public abstract void delete() throws IOException;
    }

    /**
     * A FileInfo object contains information about a file. It can be constructed from a FileAccessor, or directly from
     * data gathered through other means (i.e., cached data). Subclasses are used to store platform-specific file
     * information.  Sublcasses should implement whatever extension of IFileEx is appropriate.
     */
    public static class FileInfo implements IFileEx {
	public enum Type {
	    FILE,
	    DIRECTORY,
	    LINK;
	}

	protected long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, length=-1L;
	protected Type type = null;

	protected FileInfo() {}

	public FileInfo(FileAccessor access, Type type) throws IOException {
	    this.type = type;
	    ctime = access.getCtime();
	    mtime = access.getMtime();
	    atime = access.getAtime();
	    length = access.getLength();
	}

	public FileInfo(long ctime, long mtime, long atime, Type type, long length) {
	    this.ctime = ctime;
	    this.mtime = mtime;
	    this.atime = atime;
	    this.type = type;
	    this.length = length;
	}

	public long getCtime() {
	    return ctime;
	}

	public long getMtime() {
	    return mtime;
	}

	public long getAtime() {
	    return atime;
	}

	public long getLength() {
	    return length;
	}

	public Type getType() {
	    return type;
	}
    }

    /**
     * Default implementation of an IFile -- works with Java (local) Files.
     */
    public class DefaultFile implements IFile {
	protected String path;
	protected FileAccessor accessor;
	protected FileInfo info;
	protected Flags flags;

	public DefaultFile(File f, Flags flags) {
	    path = f.getPath();
	    this.flags = flags;
	    this.accessor = new DefaultAccessor(f);
	}

	public DefaultFile(String path, FileInfo info) {
	    this.path = path;
	    this.info = info;
	    flags = getDefaultFlags();
	}

	protected DefaultFile() {}

	public boolean exists() {
	    if (info == null) {
		try {
		    return getAccessor().exists();
		} catch (Exception e) {
		    return false;
		}
	    } else {
		return true;
	    }
	}

	public boolean isLink() throws IOException {
	    return getInfo().getType() == FileInfo.Type.LINK;
	}

	public String getLinkPath() throws IllegalStateException, IOException {
	    if (getInfo().getType() != FileInfo.Type.LINK) {
		throw new IllegalStateException(getInfo().getType().toString());
	    }
	    return getAccessor().getCanonicalPath();
	}

	public long accessTime() throws IOException {
	    return getInfo().getAtime();
	}

	public long createTime() throws IOException {
	    return getInfo().getCtime();
	}

	public final boolean mkdir() {
	    switch(flags) {
	      case READWRITE:
		try {
		    return getAccessor().mkdir();
		} catch (IOException e) {
		    return false;
		}
	      default:
		return false;
	    }
	}

	public InputStream getInputStream() throws IOException {
	    return getAccessor().getInputStream();
	}

	public final OutputStream getOutputStream(boolean append) throws IOException {
	    switch(flags) {
	      case READWRITE:
		return getAccessor().getOutputStream(append);
	      default:
		throw new AccessControlException("Method: getOutputStream, Flags: " + flags);
	    }
	}

	public final IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException {
	    if ("rw".equals(mode) && flags != IFile.Flags.READWRITE) {
		throw new AccessControlException("Method: getRandomAccess, Mode: " + mode + ", Flags: " + flags);
	    }
	    return getAccessor().getRandomAccess(mode);
	}

	public boolean isDirectory() throws IOException {
	    return getInfo().getType() == FileInfo.Type.DIRECTORY;
	}

	public boolean isFile() throws IOException {
	    return getInfo().getType() == FileInfo.Type.FILE;
	}

	public long lastModified() throws IOException {
	    return getInfo().getMtime();
	}

	public long length() throws IOException {
	    return getInfo().getLength();
	}

	public String[] list() throws IOException {
	    if (isDirectory()) {
		String[] list = getAccessor().list();
		if (list == null) {
		    return new String[0];
		} else {
		    return list;
		}
	    } else {
		return null;
	    }
	}

	public IFile[] listFiles() throws IOException {
	    return listFiles(null);
	}

	public IFile[] listFiles(Pattern p) throws IOException {
	    String[] fnames = list();
	    if (fnames == null) {
		return null;
	    }
	    List<IFile> result = new ArrayList<IFile>();
	    for (String fname : fnames) {
		if (p == null || p.matcher(fname).find()) {
		    result.add(getFile(path + DELIM + fname, flags));
		}
	    }
	    return result.toArray(new IFile[result.size()]);
	}

	public IFile getChild(String name) throws IOException {
	    if (isDirectory()) {
		return getFile(path + DELIM + name, flags);
	    } else {
		throw new IOException("Not a directory: " + path);
	    }
	}

	public void delete() throws IOException {
	    switch(flags) {
	      case READWRITE:
		getAccessor().delete();
		break;
	      default:
		throw new AccessControlException("Method: delete, Flags: " + flags);
	    }
	}

	public String getPath() {
	    return path;
	}

	public String getCanonicalPath() throws IOException {
	    return getAccessor().getCanonicalPath();
	}

	public String getName() {
	    int ptr = path.lastIndexOf(DELIM);
	    if (ptr == -1) {
		return path;
	    } else {
		return path.substring(ptr+1);
	    }
	}

	public String getParent() {
	    int ptr = path.lastIndexOf(DELIM);
	    if (ptr == -1) {
		return path;
	    } else if (ptr == 0) {
		return DELIM;
	    } else {
		return path.substring(0,ptr);
	    }
	}

	public IFileEx getExtended() throws IOException {
	    return getInfo();
	}

	// Internal

	protected FileAccessor getAccessor() throws IOException {
	    if (accessor == null) {
		accessor = new DefaultAccessor(new File(path));
	    }
	    return accessor;
	}

	protected FileInfo getInfo() throws IOException {
	    if (info == null) {
		info = getAccessor().getInfo();
	    }
	    return info;
	}
    }

    /**
     * FileAccessor implementation for Java (local) Files.
     */
    public class DefaultAccessor extends FileAccessor {
	protected File file;

	protected DefaultAccessor(File file) {
	    this.file = file;
	}

	public String toString() {
	    return file.toString();
	}

	// Implement abstract methods from FileAccessor

	public boolean exists() {
	    return file.exists();
	}

	public void delete() {
	    file.delete();
	}

	public FileInfo getInfo() throws IOException {
	    if (exists()) {
		long ctime = getCtime();
		long mtime = getMtime();
		long atime = getAtime();
		long length = getLength();

		// Determine the file type...

		FileInfo.Type type = FileInfo.Type.FILE;

		File canon;
		if (file.getParent() == null) {
		    canon = file;
		} else {
		    File canonDir = file.getParentFile().getCanonicalFile();
		    canon = new File(canonDir, file.getName());
		}

		if (!canon.getCanonicalFile().equals(canon.getAbsoluteFile())) {
		    type = FileInfo.Type.LINK;
		} else if (file.isDirectory()) {
		    type = FileInfo.Type.DIRECTORY;
		}

		return new FileInfo(ctime, mtime, atime, type, length);
	    } else {
		throw new FileNotFoundException(file.getPath());
	    }
	}

	public long getCtime() throws IOException {
	    return IFile.UNKNOWN_TIME;
	}

	public long getMtime() throws IOException {
	    return file.lastModified();
	}

	public long getAtime() throws IOException {
	    return IFile.UNKNOWN_TIME;
	}

	public long getLength() throws IOException {
	    return file.length();
	}

	public IRandomAccess getRandomAccess(String mode) throws IOException {
	    return new RandomAccessImpl(new RandomAccessFile(file, mode));
	}

	public InputStream getInputStream() throws IOException {
	    return new FileInputStream(file);
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
	    return new FileOutputStream(file, append);
	}

	public String getCanonicalPath() throws IOException {
	    return file.getCanonicalPath();
	}

	public String[] list() throws IOException {
	    return file.list();
	}

	public boolean mkdir() {
	    return file.mkdir();
	}
    }

    // Internal

    /**
     * Implementation of IRandomAccess for the DefaultFile.
     */
    class RandomAccessImpl implements IRandomAccess {
	private RandomAccessFile raf;

	public RandomAccessImpl(RandomAccessFile raf) {
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
