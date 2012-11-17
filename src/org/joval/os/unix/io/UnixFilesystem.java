// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.CacheFilesystem;
import org.joval.io.fs.DefaultFile;
import org.joval.io.fs.FileInfo;
import org.joval.io.BufferedReader;
import org.joval.io.PerishableReader;
import org.joval.io.StreamTool;
import org.joval.os.unix.io.driver.AIXDriver;
import org.joval.os.unix.io.driver.LinuxDriver;
import org.joval.os.unix.io.driver.MacOSXDriver;
import org.joval.os.unix.io.driver.SolarisDriver;
import org.joval.util.tree.Tree;
import org.joval.util.tree.Node;
import org.joval.util.JOVALMsg;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;

/**
 * A local IFilesystem implementation for Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFilesystem extends CacheFilesystem implements IUnixFilesystem {
    protected final static String LOCAL_INDEX		= "fs.index.gz";
    protected final static String INDEX_PROPS		= "fs.index.properties";
    protected final static String INDEX_PROP_COMMAND	= "command";
    protected final static String INDEX_PROP_USER	= "user";
    protected final static String INDEX_PROP_FLAVOR	= "flavor";
    protected final static String INDEX_PROP_MOUNTS	= "mounts";
    protected final static String INDEX_PROP_LEN	= "length";
    protected final static String DELIM_STR		= "/";
    protected final static char   DELIM_CH		= '/';

    protected long S, M, L, XL;
    protected boolean preloaded = false;
    protected IUnixSession us;

    private IUnixFilesystemDriver driver;
    private Collection<IMount> mounts;

    public UnixFilesystem(IUnixSession session) {
	super(session, "/");
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

    public Collection<IMount> getMounts(Pattern filter) throws IOException {
	if (mounts == null) {
	    try {
		mounts = getDriver().getMounts(null);
	    } else {
		mounts = getDriver().getMounts(Pattern.compile(fsTypeFilter));
	    }
	    for (String mount : mounts) {
		addRoot(mount);
	    }

	    String command = getDriver().getFindCommand();

	    if (VAL_FILE_METHOD.equals(props.getProperty(PROP_PRELOAD_METHOD))) {
		//
		// The file method stores the output of the find command in a file...
		//
		IReader reader = null;
		File wsdir = session.getWorkspace();
		IFile remoteCache = null, propsFile = null;
		boolean cleanRemoteCache = true;
		if(wsdir == null) {
		    //
		    // State cannot be saved locally, so create the file on the remote machine.
		    //
		    cleanRemoteCache = false;
		    remoteCache = getRemoteCache(command, mounts);
		    propsFile = getRemoteCacheProps();
		    reader = new BufferedReader(new GZIPInputStream(remoteCache.getInputStream()));
		} else {
		    //
		    // Read from the local state file, or create one while reading from the remote state file.
		    //
		    File local = new File(wsdir, LOCAL_INDEX);
		    IFile localCache = new DefaultFile(this, local, local.getAbsolutePath());
		    File localProps = new File(wsdir, INDEX_PROPS);
		    propsFile = new DefaultFile(this, localProps, localProps.getAbsolutePath());
		    Properties cacheProps = new Properties();
		    if (propsFile.exists()) {
			cacheProps.load(propsFile.getInputStream());
		    }
		    if (isValidCache(localCache, new PropertyUtil(cacheProps), command, mounts)) {
			reader = new BufferedReader(new GZIPInputStream(localCache.getInputStream()));
			cleanRemoteCache = false;
		    } else {
			remoteCache = getRemoteCache(command, mounts);
			OutputStream out = localCache.getOutputStream(false);
			StreamTool.copy(remoteCache.getInputStream(), out);
			out.close();
			reader = new BufferedReader(new GZIPInputStream(localCache.getInputStream()));
		    }
		}

		//
		// Store properties about the remote cache file.  If there is none, then we're using the verified local
		// cache file, so there's no new data to store.
		//
		if (remoteCache != null) {
		    Properties cacheProps = new Properties();
		    cacheProps.setProperty(INDEX_PROP_COMMAND, command);
		    cacheProps.setProperty(INDEX_PROP_USER, us.getEnvironment().getenv("LOGNAME"));
		    cacheProps.setProperty(INDEX_PROP_FLAVOR, us.getFlavor().value());
		    cacheProps.setProperty(INDEX_PROP_MOUNTS, alphabetize(mounts));
		    cacheProps.setProperty(INDEX_PROP_LEN, Long.toString(remoteCache.length()));
		    cacheProps.store(propsFile.getOutputStream(false), null);
		}

		addEntries(reader);
		if (cleanRemoteCache) {
		    remoteCache.delete();
		    if (remoteCache.exists()) {
			SafeCLI.exec("rm -f " + remoteCache.getPath(), session, IUnixSession.Timeout.S);
		    }
		}
	    } else {
		//
		// The stream method (default) reads directly from the stdout of the find command on the remote host.
		//
		for (String mount : mounts) {
		    IProcess p = null;
		    ErrorReader er = null;
		    IReader reader = null;
		    try {
			p = session.createProcess(command.replace("%MOUNT%", mount), null);
			p.start();
			reader = PerishableReader.newInstance(p.getInputStream(), S);
			er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
			er.start();
			addEntries(reader);
		    } finally {
			//
			// Clean-up
			//
			if (p != null) {
			    p.waitFor(0);
			}
			if (er != null) {
			    er.join();
			}
		    }
		}
	    }
	    preloaded = true;
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_DONE, cacheSize());
	    return true;
	} catch (PreloadOverflowException e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD);
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    /**
     * This is really just an ersatz local test -- it merely checks whether the path is in the cache -- but it's
     * close enough for the purpose of avoiding performance-intensive probing of remote filesystems.
     */
    public boolean isLocalPath(String path) {
	loadCache();
	try {
	    peek(path);
	    return true;
	} catch (NoSuchElementException e) {
	    return false;
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
	protected UnixFile() {}

	UnixFile(File file, IFile.Flags flags) throws IOException {
	    path = file.getPath();
	    this.flags = flags;
	    accessor = new UnixAccessor(file);
	}

	/**
	 * Create a UnixFile using information.
	 */
	protected UnixFile(UnixFileInfo info) {
	    super(info.getPath(), info);
	}
    }

    /**
     * Create a UnixFile from the output line of the stat command.
     */
    protected UnixFileInfo getUnixFileInfo(String path) throws Exception {
	String cmd = new StringBuffer(getDriver().getStatCommand()).append(" ").append(path).toString();
	List<String> lines = SafeCLI.multiLine(cmd, us, IUnixSession.Timeout.S);
	UnixFileInfo ufi = null;
	if (lines.size() > 0) {
	    ufi = (UnixFileInfo)getDriver().nextFileInfo(lines.iterator());
	    if (ufi == null) {
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIXFILEINFO, path, lines.get(0)));
	    }
	} else {
	    logger.warn(JOVALMsg.ERROR_UNIXFILEINFO, path, "''");
	}
	return ufi;
    }

    // Private

    /**
     * Read entries from the cache file.
     */
    private void addEntries(IReader reader) throws PreloadOverflowException, IOException {
	UnixFileInfo ufi = null;
	ReaderIterator iter = new ReaderIterator(reader);
	while((ufi = (UnixFileInfo)getDriver().nextFileInfo(iter)) != null) {
	    if (ufi.path == null) {
		// bad entry -- skip it!
	    } else if (entries++ < maxEntries) {
		if (entries % 20000 == 0) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_FILE_PROGRESS, entries);
		}
		addToCache(ufi.path, new UnixFile(this, ufi, ufi.path));
	    } else {
		logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
		throw new PreloadOverflowException();
	    }
	}

	@Override
	public String toString() {
	    return getPath();
	}

	@Override
        protected FileAccessor getAccessor() throws IOException {
            if (accessor == null) {
                accessor = new UnixAccessor(new File(path));
            }
            return accessor;
        }

	@Override
	public boolean isDirectory() throws IOException {
	    if (isLink()) {
		return getFile(getCanonicalPath()).isDirectory();
	    } else {
		return super.isDirectory();
	    }
	} catch (FileNotFoundException e) {
	}
    }

    // Protected

    protected UnixFileInfo getUnixFileInfo(String path) throws IOException {
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
	    return ufi;
	} catch (Exception e) {
	    if (e instanceof IOException) {
		throw (IOException)e;
	    } else {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new IOException(e.getMessage());
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
	public FileInfo getInfo() throws IOException {
	    FileInfo result = getUnixFileInfo(path);
	    if (result == null) {
		result = super.getInfo();
	    }
	    return result;
	}
    }
}
