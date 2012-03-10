// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.File;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;
import org.joval.intf.util.tree.INode;
import org.joval.io.fs.CacheFile;
import org.joval.io.fs.CacheFilesystem;
import org.joval.io.fs.DefaultFile;
import org.joval.io.fs.FileInfo;
import org.joval.io.PerishableReader;
import org.joval.io.StreamLogger;
import org.joval.util.tree.Tree;
import org.joval.util.tree.Node;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.PropertyUtil;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;
import org.joval.util.tree.TreeHash;

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

    private IUnixSession us;
    private int entries, maxEntries;

    public UnixFilesystem(IBaseSession session, IEnvironment env) {
	super(session, env, null, DELIM_STR);
	us = (IUnixSession)session;
	S = us.getTimeout(IUnixSession.Timeout.S);
	M = us.getTimeout(IUnixSession.Timeout.M);
	L = us.getTimeout(IUnixSession.Timeout.L);
	XL= us.getTimeout(IUnixSession.Timeout.XL);
    }

    /**
     * Non-local subclasses should override this method.
     */
    protected String getPreloadPropertyKey() {
	return PROP_PRELOAD_LOCAL;
    }

    @Override
    protected IFile accessResource(String path) throws IllegalArgumentException, IOException {
	return new UnixFile(this, (CacheFile)super.accessResource(path), path);
    }

    @Override
    protected boolean loadCache() {
	if (!props.getBooleanProperty(getPreloadPropertyKey())) {
	    return false;
	} else if (preloaded) {
	    return true;
	} else if (session.getType() != IBaseSession.Type.UNIX) {
	    return false;
	}

	//
	// Discard any contents from an existing cache.
	//
	reset();

	entries = 0;
	maxEntries = props.getIntProperty(PROP_PRELOAD_MAXENTRIES);

	try {
	    String command = getFindCommand();
	    List<String> mounts = getMounts();

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
		    reader = PerishableReader.newInstance(new GZIPInputStream(remoteCache.getInputStream()), S);
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
			InputStream in = new GZIPInputStream(localCache.getInputStream());
			reader = PerishableReader.newInstance(in, S);
			cleanRemoteCache = false;
		    } else {
			remoteCache = getRemoteCache(command, mounts);
			InputStream tee = new StreamLogger(null, remoteCache.getInputStream(), localCache, logger);
			reader = PerishableReader.newInstance(new GZIPInputStream(tee), S);
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
			p = session.createProcess(command.replace("%MOUNT%", mount));
			logger.info(JOVALMsg.STATUS_PROCESS_START, p.getCommand());
			p.start();
			reader = PerishableReader.newInstance(p.getInputStream(), S);
			er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
			er.start();
			addEntries(reader);
		    } finally {
			if (reader != null) {
			    reader.close();
			}
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
	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_DONE, countCacheItems());
	    return true;
	} catch (PreloadOverflowException e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
	    preloaded = true;
	    return true;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.ERROR_PRELOAD);
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    return false;
	}
    }

    // Internal

    /**
     * Create a UnixFile from the output line of the stat command.
     */
    UnixFileInfo getUnixFileInfo(String path) {
	String command = getStatCommand() + path;
	try {
	    return getUnixFileInfoFromLstat(SafeCLI.exec(command, session, IUnixSession.Timeout.S));
	} catch (Exception e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return null;
    }

    // Private

    /**
     * Generate a UnixFileInfo based on the output of an ls command.
     */
    private UnixFileInfo getUnixFileInfoFromLstat(String line) {
	char unixType = line.charAt(0);
	String permissions = line.substring(1, 10);
	boolean hasExtendedAcl = false;
	if (line.charAt(10) == '+') {
	    hasExtendedAcl = true;
	}

	StringTokenizer tok = new StringTokenizer(line.substring(11));
	String linkCount = tok.nextToken();
	int uid = -1;
	try {
	    uid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}
	int gid = -1;
	try {
	    gid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}

	FileInfo.Type type = FileInfo.Type.FILE;
	switch(unixType) {
	  case IUnixFileInfo.DIR_TYPE:
	    type = FileInfo.Type.DIRECTORY;
	    break;

	  case IUnixFileInfo.LINK_TYPE:
	    type = FileInfo.Type.LINK;
	    break;

	  case IUnixFileInfo.CHAR_TYPE:
	  case IUnixFileInfo.BLOCK_TYPE:
	    int ptr = -1;
	    if ((ptr = line.indexOf(",")) > 11) {
		tok = new StringTokenizer(line.substring(ptr+1));
	    }
	    break;
	}

	long length = 0;
	try {
	    length = Long.parseLong(tok.nextToken());
	} catch (NumberFormatException e) {
	}

	long mtime = FileInfo.UNKNOWN_TIME;
	String dateStr = tok.nextToken("/").trim();
	try {
	    if (dateStr.indexOf(":") == -1) {
		switch(us.getFlavor()) {
		  case SOLARIS:
		  case LINUX:
		    mtime = new SimpleDateFormat("MMM dd  yyyy").parse(dateStr).getTime();
		    break;

		  case AIX:
		  default:
		    mtime = new SimpleDateFormat("MMM dd yyyy").parse(dateStr).getTime();
		    break;
		}
	    } else {
		mtime = new SimpleDateFormat("MMM dd HH:mm").parse(dateStr).getTime();
	    }
	} catch (ParseException e) {
	    e.printStackTrace();
	}

	String path = null, linkPath = null;
	int begin = line.indexOf(DELIM_STR);
	if (begin > 0) {
	    int end = line.indexOf("->");
	    if (end == -1) {
	        path = line.substring(begin).trim();
	    } else if (end > begin) {
	        path = line.substring(begin, end).trim();
		linkPath = line.substring(end+2).trim();
	    }
	}

	return new UnixFileInfo(FileInfo.UNKNOWN_TIME, mtime, FileInfo.UNKNOWN_TIME, type, length, linkPath,
				unixType, permissions, uid, gid, hasExtendedAcl, path);
    }

    /**
     * Create a UnixFile from an IFile.
     */
    // Private

    /**
     * Read entries from the cache file.
     */
    private void addEntries(IReader reader) throws PreloadOverflowException, IOException {
	String line = null;
	while((line = reader.readLine()) != null) {
	    if (entries++ < maxEntries) {
		if (entries % 20000 == 0) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_FILE_PROGRESS, entries);
		}
		UnixFileInfo ufi = getUnixFileInfo(line);
		addToCache(ufi.path, new UnixFile(this, ufi, ufi.path));
	    } else {
		logger.warn(JOVALMsg.ERROR_PRELOAD_OVERFLOW, maxEntries);
		throw new PreloadOverflowException();
	    }
	}
	reader.close();
    }

    /**
     * Returns some variation of the ls or stat command.  The final argument (not included) should be the escaped path of
     * the file being stat'd.
     *
     * REMIND (DAS): Superior alternatives TBD...
     */
    private String getStatCommand() {
	switch(us.getFlavor()) {
	  case LINUX:
//return "stat --format=%A,%u,%g,%s,%W,%X,%Y,%Z,%n";
	    return "ls -dn";

	  case MACOSX:
//return "stat -f %p,%u,%g,%z,%B,%a,%m,%N";
	    return "ls -ldn";

	  case SOLARIS:
//return "ls -ldnE";
	  case AIX:
//?? istat ??
//return "ls -ndN";
	    return "ls -dn";

	  default:
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor()));
	}
    }

    /**
     * Returns a string containing the correct find command for the Unix flavor.  The command contains the String "%MOUNT%"
     * where the actual path of the mount should be substituted.
     *
     * The resulting command will follow links, but restrict results to the originating filesystem.
     */
    private String getFindCommand() throws Exception {
	StringBuffer command = new StringBuffer("find %MOUNT%");
	switch(us.getFlavor()) {
	  case LINUX:
	    return command.append(" -print0 -mount | xargs -0 ").append(getStatCommand()).toString();

	  case MACOSX:
	    return command.append(" -print0 -mount | xargs -0 ").append(getStatCommand()).toString();

	  case AIX:
	    return command.append(" -xdev -exec ").append(getStatCommand()).append(" {} \\;").toString();

	  case SOLARIS:
	    return command.append(" -mount -exec ").append(getStatCommand()).append(" {} \\;").toString();

	  default:
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor()));
	}
    }

    /**
     * Get a list of mount points.  The result will be filtered by the configured list of filesystem types.
     */
    private List<String> getMounts() throws Exception {
	List<String> fsTypeFilter = new Vector<String>();
	String filterStr = props.getProperty(PROP_PRELOAD_FSTYPE_FILTER);
	if (filterStr != null) {
	    fsTypeFilter = StringTools.toList(StringTools.tokenize(filterStr, ":", true));
	}

	List<String> mounts = new Vector<String>();
	int lineNum = 0;
	switch(us.getFlavor()) {
	  case AIX:
	    for (String line : SafeCLI.multiLine("mount", us, S)) {
		if (lineNum++ > 1) { // skip the first two lines
		    int mpToken = 1;
		    switch(line.charAt(0)) {
		      case ' ':
		      case '\t':
			mpToken = 1; // mount-point is the second token
			break;
		      default:
			mpToken = 2; // mount-point is the third token
			break;
		    }
		    StringTokenizer tok = new StringTokenizer(line);
		    String mountPoint = null, fsType = null;
		    for (int i=0; i <= mpToken; i++) {
			mountPoint = tok.nextToken();
		    }
		    fsType = tok.nextToken();

		    if (fsTypeFilter.contains(fsType)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, mountPoint, fsType);
		    } else if (mountPoint.startsWith(DELIM_STR)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, mountPoint, fsType);
			mounts.add(mountPoint);
		    }
		}
	    }
	    break;

	  case MACOSX: {
	    StringBuffer command = new StringBuffer("df");
	    int filterSize = fsTypeFilter.size();
	    if (filterSize > 0) {
		command.append(" -T no");
		for (int i=0; i < filterSize; i++) {
		    if (i > 0) {
			command.append(",");
		    }
		    command.append(fsTypeFilter.get(i));
		}
	    }
	    for (String line : SafeCLI.multiLine(command.toString(), us, S)) {
		if (lineNum++ > 0) { // skip the first line
		    StringTokenizer tok = new StringTokenizer(line);
		    String mountPoint = null;
		    while(tok.hasMoreTokens()) {
			mountPoint = tok.nextToken();
		    }
		    if (mountPoint.startsWith(DELIM_STR)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, mountPoint, "?");
			mounts.add(mountPoint);
		    }
		}
	    }
	    break;
	  }

	  case SOLARIS: {
	    IReader reader = PerishableReader.newInstance(getFile("/etc/vfstab").getInputStream(), S);
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		if (!line.startsWith("#")) { // skip comments
		    StringTokenizer tok = new StringTokenizer(line);
		    String dev = tok.nextToken();
		    String fixdev = tok.nextToken();
		    String mountPoint = tok.nextToken();
		    String fsType = tok.nextToken();
		    if (fsTypeFilter.contains(fsType)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, mountPoint, fsType);
		    } else if (mountPoint.startsWith(DELIM_STR)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, mountPoint, fsType);
			mounts.add(mountPoint);
		    }
		}
	    }
	    break;
	  }

	  case LINUX:
	    for (String line : SafeCLI.multiLine("df -TP", us, S)) {
		if (lineNum++ > 0) { // skip the first line
		    StringTokenizer tok = new StringTokenizer(line);
		    String fsName = tok.nextToken();
		    String fsType = tok.nextToken();
		    String mountPoint = null;
		    while(tok.hasMoreTokens()) {
			mountPoint = tok.nextToken();
		    }
		    if (fsTypeFilter.contains(fsType)) {
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_SKIP, mountPoint, fsType);
		    } else if (mountPoint.startsWith(DELIM_STR) && !mounts.contains(mountPoint)) { // skip over-mounts
			logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, mountPoint, fsType);
			mounts.add(mountPoint);
		    }
		}
	    }
	    break;
	}
	return mounts;
    }

    /**
     * Check to see if the IFile represents a valid cache of the preload data.  This works on either a local or remote
     * copy of the cache.  The lastModified date is compared against the expiration, and if stale, the IFile is deleted.
     *
     * After the date is checked, the properties are used to validate the length of the file and the list of filesystem
     * mounts to be indexed.
     */
    private boolean isValidCache(IFile f, IProperty cacheProps, String command, List<String> mounts) throws IOException {
	try {
	    test(f.exists() && f.isFile(), "isFile");

	    //
	    // Check the expiration date
	    //
	    String s = PROP_PRELOAD_MAXAGE;
	    test(System.currentTimeMillis() < (f.lastModified() + props.getLongProperty(s)), s);

	    //
	    // Check the command
	    //
	    s = INDEX_PROP_COMMAND;
	    test(command.equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the username
	    //
	    s = INDEX_PROP_USER;
	    test(us.getEnvironment().getenv("LOGNAME").equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the Unix flavor
	    //
	    s = INDEX_PROP_FLAVOR;
	    test(us.getFlavor().value().equals(cacheProps.getProperty(s)), s);

	    //
	    // Check the length
	    //
	    s = INDEX_PROP_LEN;
	    test(f.length() == cacheProps.getLongProperty(s), s);

	    //
	    // Check the mounts
	    //
	    s = INDEX_PROP_MOUNTS;
	    test(alphabetize(mounts).equals(cacheProps.getProperty(s)), s);

	    logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_REUSE, f.getPath());
	    return true;
	} catch (AssertionError e) {
	    logger.warn(JOVALMsg.STATUS_FS_PRELOAD_CACHE_MISMATCH, f.getPath(), e.getMessage());
	    try {
		f.delete();
	    } catch (IOException ioe) {
	    }
	    return false;
	}
    }

    private static final String CACHE_DIR = "%HOME%";
    private static final String CACHE_TEMP = ".jOVAL.find.gz~";
    private static final String CACHE_FILE = ".jOVAL.find.gz";
    private static final String CACHE_PROPS = ".jOVAL.find.properties";

    private IFile getRemoteCacheProps() throws IOException {
	return getFile(env.expand(CACHE_DIR + DELIM_STR + CACHE_PROPS), IFile.READWRITE);
    }

    /**
     * Return a valid cache file on the remote machine, if available, or create a new one and return it.
     */
    private IFile getRemoteCache(String command, List<String> mounts) throws Exception {
	String tempPath = env.expand(CACHE_DIR + DELIM_STR + CACHE_TEMP);
	String destPath = env.expand(CACHE_DIR + DELIM_STR + CACHE_FILE);

	Properties cacheProps = new Properties();
	IFile propsFile = getRemoteCacheProps();
	if (propsFile.exists()) {
	    cacheProps.load(propsFile.getInputStream());
	}

	IFile temp = getFile(destPath, IFile.READVOLATILE);
	if (isValidCache(temp, new PropertyUtil(cacheProps), command, mounts)) {
	    return temp;
	}

	logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_TEMP, tempPath);
	for (int i=0; i < mounts.size(); i++) {
	    StringBuffer sb = new StringBuffer(command.replace("%MOUNT%", mounts.get(i)));
	    sb.append(" | gzip ");
	    if (i > 0) {
		sb.append(">> "); // append
	    } else {
		sb.append("> ");  // over-write
	    }
	    sb.append(env.expand(tempPath)).toString();

	    IProcess p = session.createProcess(sb.toString());
	    logger.info(JOVALMsg.STATUS_PROCESS_START, p.getCommand());
	    p.start();
	    InputStream in = p.getInputStream();
	    if (in instanceof PerishableReader) {
		// This could take a while!
		((PerishableReader)in).setTimeout(XL);
	    }
	    ErrorReader er = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
	    er.start();

	    //
	    // Log a status update every 15 seconds while we wait, but wait for no more than an hour.
	    //
	    boolean done = false;
	    for (int j=0; !done && j < 240; j++) {
		for (int k=0; !done && k < 15; k++) {
		    if (p.isRunning()) {
			Thread.sleep(1000);
		    } else {
			done = true;
		    }
		}
		logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_PROGRESS, getFile(tempPath, IFile.READVOLATILE).length());
	    }
	    if (!done) {
		p.destroy();
	    }
	    in.close();
	    er.close();
	    er.join();
	}

	SafeCLI.exec("mv " + tempPath + " " + destPath, session, S);
	logger.info(JOVALMsg.STATUS_FS_PRELOAD_CACHE_CREATE, destPath);
	return getFile(destPath);
    }

    /**
     * Pretty much exactly the same as an "assert" statement.
     */
    private void test(boolean val, String msg) throws AssertionError {
	if (!val) throw new AssertionError(msg);
    }

    private String alphabetize(List<String> sc) {
	String[] sa = sc.toArray(new String[sc.size()]);
	Arrays.sort(sa);
	StringBuffer sb = new StringBuffer();
	for (String s : sa) {
	    if (sb.length() > 0) {
		sb.append(":");
	    }
	    sb.append(s);
	}
	return sb.toString();
    }

    private class ErrorReader implements Runnable {
	IReader err;
	Thread t;

	ErrorReader(IReader err) {
	    err.setLogger(logger);
	    this.err = err;
	}

	void start() {
	    t = new Thread(this);
	    t.start();
	}

	void join() throws InterruptedException {
	    t.join();
	}

	void close() {
	    if (t.isAlive()) {
		t.interrupt();
	    }
	}

	public void run() {
	    try {
		String line = null;
		while((line = err.readLine()) != null) {
		    logger.warn(JOVALMsg.ERROR_PRELOAD_LINE, line);
		}
	    } catch (InterruptedIOException e) {
		// ignore
	    } catch (IOException e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		try {
		    err.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
