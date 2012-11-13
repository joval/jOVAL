// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.io.AbstractFilesystem;
import org.joval.io.BufferedReader;
import org.joval.io.PerishableReader;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * An interface for searching a hierarchy.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixFileSearcher implements ISearchable<IFile>, ISearchable.ISearchPlugin<IFile>, ILoggable {
    /**
     * Static map of filesystem instances indexed by hashcode for reference by deserialized serializers.
     */
    private static Map<Integer, AbstractFilesystem> fsInstances = new HashMap<Integer, AbstractFilesystem>();

    private IUnixSession session;
    private IUnixFilesystemDriver driver;
    private AbstractFilesystem fs;
    private LocLogger logger;
    private DB db;
    private Map<String, Collection<String>> searchMap;
    private Map<String, IFile> infoCache;

    public UnixFileSearcher(IUnixSession session, IUnixFilesystemDriver driver) {
	this.session = session;
	this.driver = driver;
	fs = (AbstractFilesystem)session.getFilesystem();
	logger = session.getLogger();
	if (session.getProperties().getBooleanProperty(IFilesystem.PROP_CACHE_JDBM)) {
	    for (File f : session.getWorkspace().listFiles()) {
		if (f.getName().startsWith("fs")) {
		    f.delete();
		}
	    }
	    DBMaker dbm = DBMaker.openFile(new File(session.getWorkspace(), "fs").toString());
	    dbm.disableTransactions();
	    dbm.closeOnExit();
	    dbm.deleteFilesAfterClose();
	    db = dbm.make();
	    Integer instanceKey = new Integer(fs.hashCode());
	    if (!fsInstances.containsKey(instanceKey)) {
		fsInstances.put(instanceKey, fs);
	    }
	    infoCache = db.createHashMap("info", null, new FileSerializer(instanceKey));
	    searchMap = db.createHashMap("searches");
	} else {
	    infoCache = new HashMap<String, IFile>();
	    searchMap = new HashMap<String, Collection<String>>();
	}
    }

    public void close() {
	if (db != null) {
	    db.close();
	}
    }

    // Implement ILogger

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ISearchable<IFile>

    public Collection<IFile> search(String from, Pattern p, int maxDepth, int flags, ISearchPlugin<IFile> plugin)
		throws Exception {

	String cmd = driver.getFindCommand(from, maxDepth, flags, p == null ? null : p.pattern());
	Collection<IFile> results = new ArrayList<IFile>();
	if (searchMap.containsKey(cmd)) {
	    for (String path : searchMap.get(cmd)) {
		results.add(infoCache.get(path));
	    }
	} else {
	    logger.debug(JOVALMsg.STATUS_FS_SEARCH_START, p == null ? "null" : p.pattern(), from, flags);
	    File localTemp = null;
	    IFile remoteTemp = null;
	    Collection<String> paths = new ArrayList<String>();
	    try {
		//
		// Run the command on the remote host, storing the results in a temporary file, then tranfer the file
		// locally and read it.
		//
		IReader reader = null;
		remoteTemp = execToFile(cmd);
		if(session.getWorkspace() == null) {
		    //
		    // State cannot be saved locally, so the result will have to be buffered into memory
		    //
		    reader = new BufferedReader(new GZIPInputStream(remoteTemp.getInputStream()));
		} else {
		    //
		    // Read from the local state file, or create one while reading from the remote state file.
		    //
		    localTemp = File.createTempFile("search", null, session.getWorkspace());
		    StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(localTemp), true);
		    reader = new BufferedReader(new GZIPInputStream(new FileInputStream(localTemp)));
		}

		IFile file = null;
		Iterator<String> iter = new ReaderIterator(reader);
		while ((file = plugin.createObject(iter)) != null) {
		    String path = file.getPath();
		    logger.debug(JOVALMsg.STATUS_FS_SEARCH_MATCH, path);
		    results.add(file);
		    paths.add(path);
		    infoCache.put(path, file);
		}
		logger.info(JOVALMsg.STATUS_FS_SEARCH_DONE, results.size(), p == null ? "[none]" : p.pattern(), from);
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_FS_SEARCH);
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		if (localTemp != null) {
		    localTemp.delete();
		}
		if (remoteTemp != null) {
		    try {
			remoteTemp.delete();
			if (remoteTemp.exists()) {
			    SafeCLI.exec("rm -f " + remoteTemp.getPath(), session, IUnixSession.Timeout.S);
			}
		    } catch (Exception e) {
			logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	    searchMap.put(cmd, paths);
	}
	return results;
    }

    // Implement ISearchable.ISearchPlugin<IFile>

    public String getSubcommand() {
	return driver.getStatCommand();
    }

    public IFile createObject(Iterator<String> input) {
	UnixFileInfo info = (UnixFileInfo)driver.nextFileInfo(input);
	if (info == null) {
	    return null;
	} else if (info.getPath() == null) {
	    //
	    // Skip a bad entry and try again
	    //
	    return createObject(input);
	} else {
	    return fs.createFileFromInfo(info.getPath(), info);
	}
    }

    // Private

    /**
     * Run the command, sending its output to a temporary file, and return the temporary file.
     */
    private IFile execToFile(String command) throws Exception {
	String unique = null;
	synchronized(this) {
	    unique = Long.toString(System.currentTimeMillis());
	    Thread.sleep(1);
	}
	IEnvironment env = session.getEnvironment();
	String tempPath = env.expand("%HOME%" + IUnixFilesystem.DELIM_STR + ".jOVAL.find" + unique + ".gz");
	logger.info(JOVALMsg.STATUS_FS_SEARCH_CACHE_TEMP, tempPath);

	String cmd = new StringBuffer(command).append(" | gzip > ").append(env.expand(tempPath)).toString();
	IProcess p = session.createProcess(cmd, null);
	p.start();

	//
	// We have redirected stdout to the file, but we don't know which stream stderr can be read from, so
	// we'll try reading from both!
	//
	long XL = session.getTimeout(IUnixSession.Timeout.XL);
	ErrorReader er1 = new ErrorReader(PerishableReader.newInstance(p.getInputStream(), XL));
	er1.start();
	ErrorReader er2 = new ErrorReader(PerishableReader.newInstance(p.getErrorStream(), XL));
	er2.start();

	//
	// Log a status update every 15 seconds while we wait, but wait for no more than an hour.
	//
	boolean done = false;
	for (int i=0; !done && i < 240; i++) {
	    for (int j=0; !done && j < 15; j++) {
		if (p.isRunning()) {
		    Thread.sleep(1000);
		} else {
		    done = true;
		}
	    }
	    long len = fs.getFile(tempPath, IFile.Flags.READVOLATILE).length();
	    logger.info(JOVALMsg.STATUS_FS_SEARCH_CACHE_PROGRESS, len);
	}
	if (done) {
	    p.exitValue();
	} else {
	    p.destroy();
	}
	er1.close();
	er2.close();
	er1.join();
	er2.join();

	return fs.getFile(tempPath, IFile.Flags.READWRITE);
    }

    class ReaderIterator implements Iterator<String> {
	IReader reader;
	String next = null;

	ReaderIterator(IReader reader) {
	    this.reader = reader;
	}

	// Implement Iterator<String>

	public boolean hasNext() {
	    if (next == null) {
		try {
		    next = next();
		    return true;
		} catch (NoSuchElementException e) {
		    return false;
		}
	    } else {
		return true;
	    }
	}

	public String next() throws NoSuchElementException {
	    if (next == null) {
		try {
		    if ((next = reader.readLine()) == null) {
			try {
			    reader.close();
			} catch (IOException e) {
			}
			throw new NoSuchElementException();
		    }
		} catch (IOException e) {
		    throw new NoSuchElementException(e.getMessage());
		}
	    }
	    String temp = next;
	    next = null;
	    return temp;
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    class ErrorReader implements Runnable {
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
		    logger.warn(JOVALMsg.ERROR_FS_SEARCH_LINE, line);
		}
	    } catch (InterruptedIOException e) {
		// ignore
	    } catch (IOException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		try {
		    err.close();
		} catch (IOException e) {
		}
	    }
	}
    }
    /**
     * JDBM Serilizer implementation for Unix IFiles
     */
    static class FileSerializer implements Serializer<IFile>, Serializable {
	static final int SER_FILE = 0;
	static final int SER_DIRECTORY = 1;
	static final int SER_LINK = 2;

	Integer instanceKey;
	transient AbstractFilesystem fs;

	/**
	 * The serializer relies on an active IFilesystem, which cannot be serialized, so we serialize the hashcode
	 * of the IFilesystem, and maintain a static Map in the parent class. 
	 */
	public FileSerializer(Integer instanceKey) {
	    this.instanceKey = instanceKey;
	}

	// Implement Serializer<IFile>

	public IFile deserialize(DataInput in) throws IOException {
	    String path = in.readUTF();
	    String linkTarget = null;
	    long ctime = in.readLong();
	    long mtime = in.readLong();
	    long atime = in.readLong();
	    AbstractFilesystem.FileInfo.Type type = AbstractFilesystem.FileInfo.Type.FILE;
	    switch(in.readInt()) {
	      case SER_DIRECTORY:
		type = AbstractFilesystem.FileInfo.Type.DIRECTORY;
		break;
	      case SER_LINK:
		type = AbstractFilesystem.FileInfo.Type.LINK;
		linkTarget = in.readUTF();
		break;
	    }
	    long len = in.readLong();
	    char unixType = in.readChar();
	    String permissions = in.readUTF();
	    int uid = in.readInt();
	    int gid = in.readInt();
	    boolean hasExtendedAcl = in.readBoolean();
	    Properties extended = null;
	    if (in.readBoolean()) {
		extended = new Properties();
		int propertyCount = in.readInt();
		for (int i=0; i < propertyCount; i++) {
		    extended.setProperty(in.readUTF(), in.readUTF());
		}
	    }
	    UnixFileInfo info = new UnixFileInfo(ctime, mtime, atime, type, len, path, linkTarget, unixType,
						 permissions, uid, gid, hasExtendedAcl, extended);
	    return fs.createFileFromInfo(path, info);
	}

	public void serialize(DataOutput out, IFile f) throws IOException {
	    out.writeUTF(f.getPath());
	    out.writeLong(f.createTime());
	    out.writeLong(f.lastModified());
	    out.writeLong(f.accessTime());
	    IUnixFileInfo info = (IUnixFileInfo)f.getExtended();
	    if (f.isLink()) {
		out.writeInt(SER_LINK);
		out.writeUTF(info.getLinkPath());
	    } else if (f.isDirectory()) {
		out.writeInt(SER_DIRECTORY);
	    } else {
		out.writeInt(SER_FILE);
	    }
	    out.writeLong(f.length());

	    String unixType = info.getUnixFileType();
	    if (IUnixFileInfo.FILE_TYPE_DIR.equals(unixType)) {
		out.writeChar(IUnixFileInfo.DIR_TYPE);
	    } else if (IUnixFileInfo.FILE_TYPE_FIFO.equals(unixType)) {
		out.writeChar(IUnixFileInfo.FIFO_TYPE);
	    } else if (IUnixFileInfo.FILE_TYPE_LINK.equals(unixType)) {
		out.writeChar(IUnixFileInfo.LINK_TYPE);
	    } else if (IUnixFileInfo.FILE_TYPE_BLOCK.equals(unixType)) {
		out.writeChar(IUnixFileInfo.BLOCK_TYPE);
	    } else if (IUnixFileInfo.FILE_TYPE_CHAR.equals(unixType)) {
		out.writeChar(IUnixFileInfo.CHAR_TYPE);
	    } else if (IUnixFileInfo.FILE_TYPE_SOCK.equals(unixType)) {
		out.writeChar(IUnixFileInfo.SOCK_TYPE);
	    } else {
		out.writeChar(IUnixFileInfo.FILE_TYPE);
	    }

	    out.writeUTF(info.getPermissions());
	    out.writeInt(info.getUserId());
	    out.writeInt(info.getGroupId());
	    out.writeBoolean(info.hasExtendedAcl());
	    String[] extendedKeys = info.getExtendedKeys();
	    if (extendedKeys == null) {
		out.writeBoolean(false);
	    } else {
		out.writeBoolean(true);
		out.writeInt(extendedKeys.length);
		for (int i=0; i < extendedKeys.length; i++) {
		    out.writeUTF(extendedKeys[i]);
		    out.writeUTF(info.getExtendedData(extendedKeys[i]));
		}
	    }
	}
    }
}
