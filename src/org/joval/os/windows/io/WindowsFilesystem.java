// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.io.IWindowsFilesystemDriver;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.fs.AbstractFilesystem;
import org.joval.io.fs.DefaultMetadata;
import org.joval.io.fs.IAccessor;
import org.joval.os.windows.Timestamp;
import org.joval.os.windows.identity.ACE;
import org.joval.os.windows.identity.Directory;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * The local IFilesystem implementation for Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFilesystem extends AbstractFilesystem implements IWindowsFilesystem {
    private WindowsFileSearcher searcher;
    private final IWindowsSession.View view;
    private IRunspace runspace;
    private IWindowsFilesystemDriver driver;
    private Collection<IMount> mounts;
    private String system32, sysWOW64, sysNative;
    private boolean redirect3264;

    public WindowsFilesystem(IWindowsSession session, IWindowsFilesystemDriver driver) throws Exception {
	this(session, driver, session.getNativeView());
    }

    public WindowsFilesystem(IWindowsSession session, IWindowsFilesystemDriver driver, IWindowsSession.View view)
		throws Exception {

	super(session, DELIM_STR, IWindowsSession.View._32BIT == view ? "fs32" : "fs");
	if (view == null) {
	    view = session.getNativeView();
	}
	this.view = view;
	this.driver = driver;
	for (IRunspace runspace : session.getRunspacePool().enumerate()) {
	    if (runspace.getView() == view) {
		this.runspace = runspace;
	    }
	}
	if (runspace == null) {
	    runspace = session.getRunspacePool().spawn(view);
	}
	runspace.loadModule(WindowsFilesystem.class.getResourceAsStream("WindowsFilesystem.psm1"));
	redirect3264 = view == IWindowsSession.View._32BIT && session.getNativeView() == IWindowsSession.View._64BIT;
	String sysRoot	= session.getEnvironment().getenv("SystemRoot");
	system32	= sysRoot + DELIM_STR + "System32"  + DELIM_STR;
	sysNative	= sysRoot + DELIM_STR + "Sysnative" + DELIM_STR;
	sysWOW64	= sysRoot + DELIM_STR + "SysWOW64"  + DELIM_STR;
    }

    protected String getRealPath(String path) {
	if (redirect3264) {
	    if (path.toUpperCase().startsWith(system32.toUpperCase())) {
		return sysWOW64 + path.substring(system32.length());
	    } else if (path.toUpperCase().startsWith(sysNative.toUpperCase())) {
		return system32 + path.substring(system32.length());
	    }
	}
	return path;
    }

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	driver.setLogger(logger);
	if (searcher != null) {
	    searcher.setLogger(logger);
	}
    }

    public synchronized ISearchable<IFile> getSearcher() throws IOException {
	if (searcher == null) {
	    try {
		Map<String, Collection<String>> searchMap;
		if (db == null) {
		    searchMap = new HashMap<String, Collection<String>>();
		} else {
		    searchMap = db.createHashMap("searches");
		}
		searcher = new WindowsFileSearcher((IWindowsSession)session, runspace, searchMap);
	    } catch (IOException e) {
		throw e;
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new IOException(e);
	    }
	}
	return searcher;
    }

    public Collection<IMount> getMounts(Pattern filter) throws IOException {
	try {
	    if (mounts == null) {
		mounts = driver.getMounts(null);
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
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new IOException(e.getMessage());
	}
    }

    public Serializer<IFile> getFileSerializer(Integer instanceKey) {
	return new WindowsFileSerializer(instanceKey);
    }

    public IWindowsFilesystemDriver getDriver() {
	return driver;
    }

    @Override
    public IFile createFileFromInfo(IFileMetadata info) {
	if (info instanceof WindowsFileInfo) {
	    return new WindowsFile((WindowsFileInfo)info);
	} else {
	    return super.createFileFromInfo(info);
	}
    }

    protected IFile getPlatformFile(String path, IFile.Flags flags) throws IOException {
	return new WindowsFile(path, new File(getRealPath(path)), flags);
    }

    protected WindowsFileInfo getWindowsFileInfo(String path) throws IOException {
	try {
	    String data = runspace.invoke("Get-Item -literalPath \"" + path + "\" | Print-FileInfo");
	    return (WindowsFileInfo)driver.nextFileInfo(StringTools.toList(data.split("\r\n")).iterator());
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new IOException(e.getMessage());
	}
    }

    // Private

    class WindowsFile extends DefaultFile {
	WindowsFile(String path, File file, IFile.Flags flags) {
	    super(path, new WindowsAccessor(path, file), flags);
	}

	WindowsFile(WindowsFileInfo info) {
	    super(info, Flags.READONLY);
	}

	@Override
	protected IAccessor getAccessor() {
	    if (accessor == null) {
		accessor = new WindowsAccessor(path, new File(getRealPath(path)));
	    }
	    return accessor;
	}
    }

    class WindowsAccessor extends DefaultAccessor {
	/**
	 * Due to 32-bit redirection, the path can differ from the underlying File's path.
	 */
	private String path;

	WindowsAccessor(String path, File file) {
	    super(file);
	    this.path = path;
	}

	@Override
	public DefaultMetadata getInfo() throws IOException {
	    DefaultMetadata result = getWindowsFileInfo(path);
	    if (result == null) {
		result = super.getInfo();
	    }
	    return result;
	}
    }
}
