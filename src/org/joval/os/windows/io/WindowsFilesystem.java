// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;

import org.joval.intf.io.IFile;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.AbstractFilesystem;
import org.joval.os.windows.identity.LocalACE;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * The local IFilesystem implementation for Windows.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFilesystem extends AbstractFilesystem implements IWindowsFilesystem {
    private Collection<IMount> mounts;
    private WindowsFileSearcher searcher;
    private WOW3264FilesystemRedirector redirector;
    private IWindowsSession.View view;

    public WindowsFilesystem(IWindowsSession session) throws Exception {
	this(session, null);
    }

    public WindowsFilesystem(IWindowsSession session, IWindowsSession.View view) {
	super(session, "\\");
	if (view == null) {
	    view = session.getNativeView();
	}
	this.view = view;
	if (view == IWindowsSession.View._32BIT) {
	    redirector = new WOW3264FilesystemRedirector(session.getEnvironment());
	}
    }

    public void dispose() {
	if (searcher == null) {
	    searcher.close();
	}
    }

    public synchronized ISearchable<IFile> getSearcher() throws IOException {
	if (searcher == null) {
	    try {
		searcher = new WindowsFileSearcher((IWindowsSession)session, view);
	    } catch (IOException e) {
		throw e;
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new IOException(e);
	    }
	}
	return searcher;
    }

    @Override
    public String guessParent(Pattern p) {
	String guess = super.guessParent(p);
	if (WindowsMount.isDrive(guess)) {
	    guess = guess + DELIM_STR;
	}
	return guess;
    }

    public Collection<IMount> getMounts(Pattern filter) throws IOException {
	if (mounts == null) {
	    try {
		mounts = new ArrayList<IMount>();
		File[] roots = File.listRoots();
		for (int i=0; i < roots.length; i++) {
		    String path = roots[i].getPath();
		    mounts.add(new WindowsMount(path, FsType.typeOf(Kernel32Util.getDriveType(path))));
		}
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
    public IFile createFileFromInfo(String path, FileInfo info) {
	if (info instanceof WindowsFileInfo) {
	    return new WindowsFile(path, (WindowsFileInfo)info);
	} else {
	    return super.createFileFromInfo(path, info);
	}
    }

    public final IFile getFile(String path, IFile.Flags flags) {
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
	return new WindowsFile(realPath, new File(path), flags);
    }

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (searcher != null) {
	    searcher.setLogger(logger);
	}
    }

    public class WindowsFile extends DefaultFile {
	WindowsFile(String path, File file, IFile.Flags flags) {
	    this.path = path;
	    this.flags = flags;
	    accessor = new WindowsAccessor(file);
	}

	WindowsFile(String path, WindowsFileInfo info) {
	    this.path = path;
	    this.info = info;
	    flags = getDefaultFlags();
	}

	@Override
	protected FileAccessor getAccessor() {
	    if (accessor == null) {
		String realPath = path;
		if (redirector != null) {
		    String alt = redirector.getRedirect(path);
		    if (alt != null) {
			realPath = alt;
		    }
		}
		accessor = new WindowsAccessor(new File(realPath));
	    }
	    return accessor;
	}
    }

    // Private

    class WindowsAccessor extends DefaultAccessor implements IWindowsFileInfo {
	private String path;

	WindowsAccessor(File file) {
	    super(file);
	    path = file.getPath();
	}

	@Override
	public FileInfo getInfo() throws IOException {
	    FileInfo.Type type = file.isDirectory() ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE;
	    return new WindowsFileInfo(getCtime(), getMtime(), getAtime(), type, getLength(), this);
	}

	// Implement IWindowsFileInfo

	public int getWindowsFileType() throws IOException {
	    try {
		int dirAttr = IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY & Kernel32Util.getFileAttributes(path);
		if (IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY == dirAttr) {
		    return IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY;
		} else {
		    return Kernel32Util.getFileType(path);
		}
	    } catch (Win32Exception e) {
		logger.warn(JOVALMsg.ERROR_IO, path, e.getMessage());
		throw new IOException(e);
	    }
	}

	public IACE[] getSecurity() throws IOException {
	    try {
		WinNT.ACCESS_ACEStructure[] aces = Advapi32Util.getFileSecurity(path, false);
		IACE[] acl = new IACE[aces.length];
		for (int i=0; i < aces.length; i++) {
		    acl[i] = new LocalACE(aces[i]);
		}
		return acl;
	    } catch (Win32Exception e) {
		throw new IOException(e);
	    }
	}
    }
}
