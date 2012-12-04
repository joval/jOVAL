// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ISearchable;
import org.joval.io.fs.AbstractFilesystem;
import org.joval.io.fs.DefaultMetadata;
import org.joval.io.fs.IAccessor;
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
	super(session, DELIM_STR, "fs");
	S = session.getTimeout(IUnixSession.Timeout.S);
	M = session.getTimeout(IUnixSession.Timeout.M);
	L = session.getTimeout(IUnixSession.Timeout.L);
	XL= session.getTimeout(IUnixSession.Timeout.XL);
    }

    public ISearchable<IFile> getSearcher() {
	if (searcher == null) {
	    Map<String, Collection<String>> searchMap;
	    if (db == null) {
		searchMap = new HashMap<String, Collection<String>>();
	    } else {
		searchMap = db.createHashMap("searches");
	    }
	    searcher = new UnixFileSearcher((IUnixSession)session, getDriver(), searchMap);
	}
	return searcher;
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

    public Serializer<IFile> getFileSerializer(Integer instanceKey) {
	return new UnixFileSerializer(instanceKey);
    }

    @Override
    public IFile createFileFromInfo(IFileMetadata info) {
	if (info instanceof UnixFileInfo) {
	    return new UnixFile((UnixFileInfo)info);
	} else {
	    return super.createFileFromInfo(info);
	}
    }

    protected IFile getPlatformFile(String path, IFile.Flags flags) throws IOException {
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

    // Internal

    protected UnixFileInfo getUnixFileInfo(String path) throws IOException {
	try {
	    String cmd = new StringBuffer(getDriver().getStatCommand()).append(" ").append(path).toString();
	    SafeCLI.ExecData ed = SafeCLI.execData(cmd, null, session, S);
	    List<String> lines = ed.getLines();
	    UnixFileInfo ufi = null;
	    if (lines.size() > 0) {
		ufi = (UnixFileInfo)getDriver().nextFileInfo(lines.iterator());
		if (ufi == null) {
		    if (ed.getExitCode() == 0) {
			throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIXFILEINFO, path, lines.get(0)));
		    } else {
			String message = new String(ed.getData(), StringTools.ASCII);
			throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_FS_LSTAT, path, ed.getExitCode(), message));
		    }
		}
	    } else {
		logger.warn(JOVALMsg.ERROR_UNIXFILEINFO, path, "''");
	    }
	    return ufi;
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new IOException(e.getMessage());
	}
    }

    protected class UnixFile extends DefaultFile {
	UnixFile(File file, IFile.Flags flags) throws IOException {
	    super(file.getPath(), new UnixAccessor(file), flags);
	}

	protected UnixFile(String path, IAccessor accessor, IFile.Flags flags) {
	    super(path, accessor, flags);
	}

	/**
	 * Create a UnixFile using information.
	 */
	protected UnixFile(UnixFileInfo info) {
	    super(info, IFile.Flags.READONLY);
	}

	@Override
        protected IAccessor getAccessor() throws IOException {
            if (accessor == null) {
                accessor = new UnixAccessor(new File(path));
            }
            return accessor;
        }

	/**
	 * If this file is a link to a directory, we want this to return true.
	 */
	@Override
	public boolean isDirectory() throws IOException {
	    if (isLink()) {
		return getFile(getCanonicalPath()).isDirectory();
	    } else {
		return super.isDirectory();
	    }
	}
    }

    class UnixAccessor extends DefaultAccessor {
	private String path;

	UnixAccessor(File file) {
	    super(file);
	    path = file.getPath();
	}

	@Override
	public DefaultMetadata getInfo() throws IOException {
	    DefaultMetadata result = getUnixFileInfo(path);
	    if (result == null) {
		result = super.getInfo();
	    }
	    return result;
	}
    }
}
