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

    public void dispose() {
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

    @Override
    public IFile createFileFromInfo(String path, FileInfo info) {
	if (info instanceof UnixFileInfo) {
            return new UnixFile((UnixFileInfo)info);
	} else {
	    return super.createFileFromInfo(path, info);
	}
    }

    public IFile getFile(String path, IFile.Flags flags) throws IllegalArgumentException, IOException {
	if (autoExpand) {
	    path = env.expand(path);
	}
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
