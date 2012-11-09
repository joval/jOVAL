// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ISearchable;
import org.joval.io.AbstractFilesystem;
import org.joval.os.unix.io.driver.AIXDriver;
import org.joval.os.unix.io.driver.LinuxDriver;
import org.joval.os.unix.io.driver.MacOSXDriver;
import org.joval.os.unix.io.driver.SolarisDriver;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * A local IFilesystem implementation for Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFilesystem extends AbstractFilesystem implements IUnixFilesystem {
    protected long S, M, L, XL;

    private UnixFileSearcher searcher;
    private IUnixFilesystemDriver driver;
    private Collection<IMount> mounts;

    public UnixFilesystem(IUnixSession session) {
	super(session, File.separator);
	S = session.getTimeout(IUnixSession.Timeout.S);
	M = session.getTimeout(IUnixSession.Timeout.M);
	L = session.getTimeout(IUnixSession.Timeout.L);
	XL= session.getTimeout(IUnixSession.Timeout.XL);
    }

    public ISearchable<IFile> getSearcher() {
	if (searcher == null) {
	    searcher = new UnixFileSearcher((IUnixSession)session, getDriver());
	}
	return searcher;
    }

    public ISearchable.ISearchPlugin<IFile> getDefaultPlugin() {
	return searcher;
    }

    public Collection<IMount> getMounts(Pattern filter) throws IOException {
	if (mounts == null) {
	    try {
		mounts = getDriver().getMounts(null);
	    } catch (Exception e) {
		throw new IOException(e);
	    }
	}
	if (filter == null) {
	    return mounts;
	} else {
	    Collection<IMount> results = new ArrayList<IMount>();
	    for (IMount mount : mounts) {
		if (filter.matcher(mount.getType()).find()) {
		    logger.info(JOVALMsg.STATUS_FS_MOUNT_SKIP, mount.getPath(), mount.getType());
		} else {
		    logger.info(JOVALMsg.STATUS_FS_MOUNT_ADD, mount.getPath(), mount.getType());
		    results.add(mount);
		}
	    }
	    return results;
	}
    }

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (searcher != null) {
	    searcher.setLogger(logger);
	}
	if (driver != null) {
	    driver.setLogger(logger);
	}
    }

    public IFile getFile(String path, IFile.Flags flags) throws IllegalArgumentException, IOException {
	return new UnixFile(new File(path), flags);
    }

    // Implement IUnixFilesystem

    public IUnixFilesystemDriver getDriver() {
	if (driver == null) {
	    IUnixSession us = (IUnixSession)session;
	    switch(us.getFlavor()) {
	      case AIX:
		driver = new AIXDriver(us);
		break;
	      case MACOSX:
		driver = new MacOSXDriver(us);
		break;
	      case LINUX:
		driver = new LinuxDriver(us);
		break;
	      case SOLARIS:
		driver = new SolarisDriver(us);
		break;
	      default:
		throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor()));
	    }
	}
	return driver;
    }

    public class UnixFile extends DefaultFile {
	UnixFile(File file, IFile.Flags flags) throws IOException {
	    path = file.getPath();
	    this.flags = flags;
	    accessor = new UnixAccessor(file);
	}

	/**
	 * Create a UnixFile using information.
	 */
	UnixFile(UnixFileInfo info) {
	    super(info.getPath(), info);
	}

	@Override
	public String getLinkPath() throws IllegalStateException, IOException {
	    if (getInfo().getType() != AbstractFilesystem.FileInfo.Type.LINK) {
		throw new IllegalStateException(getInfo().getType().toString());
	    }
	    return ((UnixFileInfo)getInfo()).getLinkPath();
	}

	@Override
	public String getCanonicalPath() throws IOException {
	    if (info == null) {
		return getAccessor().getCanonicalPath();
	    } else {
		return ((UnixFileInfo)info).getCanonicalPath();
	    }
	}

	@Override
	public String toString() {
	    return getPath();
	}
    }

    public static class UnixFileInfo extends FileInfo implements IUnixFileInfo {
	static final String DELIM = "/";

	private String path;
	private String linkTarget;
	private String canonicalPath;
	private boolean hasExtendedAcl = false;
	private String permissions = null;
	private int uid, gid;
	private char unixType = FILE_TYPE;
	private Properties extended;

	public UnixFileInfo(long ctime, long mtime, long atime, Type type, long length, String path, String linkTarget,
			    char unixType, String permissions, int uid, int gid, boolean hasExtendedAcl) {
	    this(ctime, mtime, atime, type, length, path, linkTarget, unixType, permissions, uid, gid, hasExtendedAcl, null);
	}

	/**
	 * Create a UnixFile with a live IFile accessor.
	 */
	public UnixFileInfo(long ctime, long mtime, long atime, Type type, long length, String path, String linkTarget,
			    char unixType, String permissions, int uid, int gid, boolean hasExtendedAcl, Properties extended) {

	    super(ctime, mtime, atime, type, length);

	    this.path = path;
	    this.linkTarget = linkTarget;
	    this.canonicalPath = resolvePath(linkTarget);

	    this.unixType = unixType;
	    this.permissions = permissions;
	    this.uid = uid;
	    this.gid = gid;
	    this.hasExtendedAcl = hasExtendedAcl;
	    this.extended = extended; // extended data
	}

	UnixFileInfo(FileInfo info, String path) {
	    ctime = info.getCtime();
	    mtime = info.getMtime();
	    atime = info.getAtime();
	    type = info.getType();
	    length = info.getLength();
	    this.path = path;
	    permissions = "-rwxrwxrwx";
	    uid = -1;
	    gid = -1;
	}

	String getPath() {
	    return path;
	}

	String getLinkPath() {
	    return linkTarget;
	}

	String getCanonicalPath() {
	    return canonicalPath;
	}

	// Implement IUnixFileInfo

	public String getUnixFileType() {
	    switch(unixType) {
	      case DIR_TYPE:
		return FILE_TYPE_DIR;
	      case FIFO_TYPE:
		return FILE_TYPE_FIFO;
	      case LINK_TYPE:
		return FILE_TYPE_LINK;
	      case BLOCK_TYPE:
		return FILE_TYPE_BLOCK;
	      case CHAR_TYPE:
		return FILE_TYPE_CHAR;
	      case SOCK_TYPE:
		return FILE_TYPE_SOCK;
	      case FILE_TYPE:
	      default:
		return FILE_TYPE_REGULAR;
	    }
	}

	public int getUserId() {
	    return uid;
	}

	public int getGroupId() {
	    return gid;
	}

	public boolean uRead() {
	    return permissions.charAt(0) == 'r';
	}

	public boolean uWrite() {
	    return permissions.charAt(1) == 'w';
	}

	public boolean uExec() {
	    return permissions.charAt(2) != '-';
	}

	public boolean sUid() {
	    return permissions.charAt(2) == 's';
	}

	public boolean gRead() {
	    return permissions.charAt(3) == 'r';
	}

	public boolean gWrite() {
	    return permissions.charAt(4) == 'w';
	}

	public boolean gExec() {
	    return permissions.charAt(5) != '-';
	}

	public boolean sGid() {
	    return permissions.charAt(5) == 's';
	}

	public boolean oRead() {
	    return permissions.charAt(6) == 'r';
	}

	public boolean oWrite() {
	    return permissions.charAt(7) == 'w';
	}

	public boolean oExec() {
	    return permissions.charAt(8) != '-';
	}

	public boolean sticky() {
	    return permissions.charAt(8) == 't';
	}

	public boolean hasExtendedAcl() {
	    return hasExtendedAcl;
	}

	public String getExtendedData(String key) throws NoSuchElementException {
	    if (extended != null && extended.containsKey(key)) {
		return extended.getProperty(key);
	    } else {
		throw new NoSuchElementException(key);
	    }
	}

	// Private

	/**
	 * Resolve an absolute path from a relative path from a base file path.
	 *
	 * @arg target the link target, which might be a path relative to the origin.
	 *
	 * @returns an absolute path to the target
	 */
	private String resolvePath(String target) throws UnsupportedOperationException {
	    if (target == null) {
		return null;
	    } else if (target.startsWith(DELIM)) {
		return target;
	    } else {
		Stack<String> stack = new Stack<String>();
		for (String s : StringTools.toList(StringTools.tokenize(path, DELIM))) {
		    stack.push(s);
		}
		if (!stack.empty()) {
		    stack.pop();
		}
		for (String next : StringTools.toList(StringTools.tokenize(target, DELIM))) {
		    if (next.equals(".")) {
			// stay in the same place
		    } else if (next.equals("..")) {
			if (stack.empty()) {
			    // links above root stay at root
			} else {
			    stack.pop();
			}
		    } else {
			stack.push(next);
		    }
		}
		StringBuffer sb = new StringBuffer();
		while(!stack.empty()) {
		    StringBuffer elt = new StringBuffer(DELIM);
		    sb.insert(0, elt.append(stack.pop()).toString());
		}
		if (sb.length() == 0) {
		    return DELIM;
		} else {
		    return sb.toString();
		}
	    }
	}
    }

    // Protected

    class UnixAccessor extends DefaultAccessor {
	private String path;

	UnixAccessor(File file) {
	    super(file);
	    path = file.getPath();
	}

	@Override
	public FileInfo getInfo() throws IOException {
	    try {
		String cmd = new StringBuffer(getDriver().getStatCommand()).append(" ").append(path).toString();
		List<String> lines = SafeCLI.multiLine(cmd, session, IUnixSession.Timeout.S);
		UnixFileInfo ufi = null;
		if (lines.size() > 0) {
		    ufi = (UnixFileInfo)getDriver().nextFileInfo(lines.iterator());
		    if (ufi == null) {
			throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIXFILEINFO, path, lines.get(0)));
		    }
		} else {
		    logger.warn(JOVALMsg.ERROR_UNIXFILEINFO, path, "''");
		}
		if (ufi == null) {
		    ufi = new UnixFileInfo(super.getInfo(), path);
		}
		return ufi;
	    } catch (Exception e) {
		if (e instanceof IOException) {
		    throw (IOException)e;
		} else {
		    throw new IOException(e);
		}
	    }
	}
    }
}
