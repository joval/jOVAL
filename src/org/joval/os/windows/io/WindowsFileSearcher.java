// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.cal10n.LocLogger;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;
import org.apache.jdbm.Serializer;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.AbstractFilesystem;
import org.joval.io.StreamTool;
import org.joval.os.windows.Timestamp;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An interface for searching a Windows filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileSearcher implements ISearchable<IFile>, ILoggable {
    /**
     * Static map of filesystem instances indexed by hashcode for reference by deserialized serializers.
     */
    private static Map<Integer, AbstractFilesystem> fsInstances = new HashMap<Integer, AbstractFilesystem>();

    private IWindowsSession session;
    private AbstractFilesystem fs;
    private IRunspace runspace;
    private LocLogger logger;
    private DB db;
    private Map<String, Collection<String>> searchMap;
    private Map<String, IFile> infoCache;

    public WindowsFileSearcher(IWindowsSession session) throws Exception {
	this(session, null);
    }

    public WindowsFileSearcher(IWindowsSession session, IWindowsSession.View view) throws Exception {
	this.session = session;
	fs = (AbstractFilesystem)session.getFilesystem(view);
	for (IRunspace runspace : session.getRunspacePool().enumerate()) {
	    if (runspace.getView() == view) {
		this.runspace = runspace;
		break;
	    }
	}
	if (runspace == null) {
	    runspace = session.getRunspacePool().spawn(view);
	}
	runspace.loadModule(getClass().getResourceAsStream("WindowsFileSearcher.psm1"));
	logger = session.getLogger();

	if (session.getProperties().getBooleanProperty(IFilesystem.PROP_CACHE_JDBM)) {
	    String dbKey = IWindowsSession.View._32BIT == view ? "fs32" : "fs";
	    for (File f : session.getWorkspace().listFiles()) {
		if (f.getName().startsWith(dbKey)) {
		    f.delete();
		}
	    }
	    DBMaker dbm = DBMaker.openFile(new File(session.getWorkspace(), dbKey).toString());
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

    public ICondition condition(int field, int type, Object value) {
	return new GenericCondition(field, type, value);
    }

    public Collection<IFile> search(List<ISearchable.ICondition> conditions) throws Exception {
	String from = null;
	Pattern pathPattern = null, dirPattern = null, basenamePattern = null;
	String basename = null;
	int maxDepth = DEPTH_UNLIMITED;
	boolean dirOnly = false;
	for (ISearchable.ICondition condition : conditions) {
	    switch(condition.getField()) {
	      case FIELD_DEPTH:
		maxDepth = ((Integer)condition.getValue()).intValue();
		break;
	      case FIELD_FROM:
		from = (String)condition.getValue();
		break;
	      case IFilesystem.FIELD_PATH:
		pathPattern = (Pattern)condition.getValue();
		break;
	      case IFilesystem.FIELD_DIRNAME:
		dirPattern = (Pattern)condition.getValue();
		break;
	      case IFilesystem.FIELD_BASENAME:
		switch(condition.getType()) {
		  case ISearchable.TYPE_EQUALITY:
		    basename = (String)condition.getValue();
		    break;
		  case ISearchable.TYPE_PATTERN:
		    basenamePattern = (Pattern)condition.getValue();
		    break;
		}
		break;
	      case IFilesystem.FIELD_FILETYPE:
		if (IFilesystem.FILETYPE_DIR.equals(condition.getValue())) {
		    dirOnly = true;
		}
		break;
	    }
	}

	Object bnObj = basename == null ? basenamePattern : basename;
	String cmd = getFindCommand(from, maxDepth, dirOnly, pathPattern, dirPattern, bnObj);
	Collection<IFile> results = new ArrayList<IFile>();
	if (searchMap.containsKey(cmd)) {
	    for (String path : searchMap.get(cmd)) {
		results.add(infoCache.get(path));
	    }
	} else {
	    logger.debug(JOVALMsg.STATUS_FS_SEARCH_START, cmd);
	    File localTemp = null;
	    IFile remoteTemp = null;
	    InputStream in = null;
	    Collection<String> paths = new ArrayList<String>();
	    try {
		//
		// Run the command on the remote host, storing the results in a temporary file, then tranfer the file
		// locally and read it.
		//
		remoteTemp = execToFile(cmd);
		if(session.getWorkspace() == null) {
		    //
		    // State cannot be saved locally, so the result will have to be buffered into memory
		    //
		    in = new GZIPInputStream(remoteTemp.getInputStream());
		} else {
		    //
		    // Read from the local state file, or create one while reading from the remote state file.
		    //
		    localTemp = File.createTempFile("search", null, session.getWorkspace());
		    StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(localTemp), true);
		    in = new GZIPInputStream(remoteTemp.getInputStream());
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StreamTool.detectEncoding(in)));
		Iterator<String> iter = new ReaderIterator(reader);
		IFile file = null;
		while ((file = createObject(iter)) != null) {
		    String path = file.getPath();
		    logger.debug(JOVALMsg.STATUS_FS_SEARCH_MATCH, path);
		    results.add(file);
		    paths.add(path);
		    infoCache.put(path, file);
		}
		logger.info(JOVALMsg.STATUS_FS_SEARCH_DONE, results.size(), cmd);
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_FS_SEARCH);
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
		    }
		}
		if (localTemp != null) {
		    localTemp.delete();
		}
		if (remoteTemp != null) {
		    try {
			remoteTemp.delete();
		    } catch (Exception e) {
			logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	    searchMap.put(cmd, paths);
	}
	return results;
    }

    // Internal

    static final String START	= "{";
    static final String END	= "}";

    IFile createObject(Iterator<String> input) {
	IFile file = null;
	boolean start = false;
	while(input.hasNext()) {
	    String line = input.next();
	    if (line.trim().equals(START)) {
		start = true;
		break;
	    }
	}
	if (start) {
	    long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, len=-1L;
	    AbstractFilesystem.FileInfo.Type type = AbstractFilesystem.FileInfo.Type.FILE;
	    int winType = IWindowsFileInfo.FILE_TYPE_UNKNOWN;
	    Collection<IACE> aces = new ArrayList<IACE>();
	    String path = null;

	    while(input.hasNext()) {
		String line = input.next().trim();
		if (line.equals(END)) {
		    break;
		} else if (line.equals("Type: File")) {
		    winType = IWindowsFileInfo.FILE_TYPE_DISK;
		} else if (line.equals("Type: Directory")) {
		    type = AbstractFilesystem.FileInfo.Type.DIRECTORY;
		    winType = IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY;
		} else {
		    int ptr = line.indexOf(":");
		    if (ptr > 0) {
			String key = line.substring(0,ptr).trim();
			String val = line.substring(ptr+1).trim();
			if ("Path".equals(key)) {
			    path = val;
			} else {
			    try {
				if ("Ctime".equals(key)) {
				    ctime = Timestamp.getTime(new BigInteger(val));
				} else if ("Mtime".equals(key)) {
				    mtime = Timestamp.getTime(new BigInteger(val));
				} else if ("Atime".equals(key)) {
				    atime = Timestamp.getTime(new BigInteger(val));
				} else if ("Length".equals(key)) {
				    len = Long.parseLong(val);
				} else if ("ACE".equals(key)) {
				    aces.add(new InternalACE(val));
				}
			    } catch (IllegalArgumentException e) {
				logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    }
			}
		    }
		}
	    }
	    WindowsFileInfo info = new WindowsFileInfo(ctime, mtime, atime, type, len, winType, aces.toArray(new IACE[0]));
	    file = fs.createFileFromInfo(path, info);
	}
	return file;
    }

    String getFindCommand(String from, int maxDepth, boolean dirOnly, Pattern path, Pattern dirname, Object bn) {
	StringBuffer command;
	if (dirOnly || dirname != null || bn != null) {
	    command = new StringBuffer("Find-Directories");
	} else {
	    command = new StringBuffer("Find-Files");
	}
	command.append(" -Path \"");
	command.append(from);
	command.append("\" -Depth ");
	command.append(Integer.toString(maxDepth));
	if (dirname != null || bn != null) {
	    if (dirname != null) {
		command.append(" -Pattern \"");
		command.append(dirname.pattern());
		command.append("\"");
	    }
	    if (!dirOnly) {
		command.append(" | Find-Files");
	    }
	    if (bn != null) {
		if (bn instanceof Pattern) {
		    command.append(" -Filename \"");
		    command.append(((Pattern)bn).pattern());
		} else if (bn instanceof String) {
		    command.append(" -LiteralFilename \"");
		    command.append((String)bn);
		}
		command.append("\"");
	    }
	} else if (path != null) {
	    command.append(" -Pattern \"");
	    command.append(path.pattern());
	    command.append("\"");
	}
	command.append(" | Print-FileInfo");
	return command.toString();
    }

    boolean isSetFlag(int flag, int flags) {
	return flag == (flag & flags);
    }

    /**
     * Run the command, sending its output to a temporary file, and return the temporary file.
     */
    private IFile execToFile(String command) throws Exception {
	String unique = null;
	synchronized(this) {
	    unique = Long.toString(System.currentTimeMillis());
	    Thread.sleep(1);
	}
	String tempPath = session.getTempDir();
	if (!tempPath.endsWith(IWindowsFilesystem.DELIM_STR)) {
	    tempPath = tempPath + IWindowsFilesystem.DELIM_STR;
	}
	tempPath = tempPath + "find." + unique + ".out";
	tempPath = session.getEnvironment().expand(tempPath);
	logger.debug(JOVALMsg.STATUS_FS_SEARCH_CACHE_TEMP, tempPath);

	String cmd = new StringBuffer(command).append(" | Out-File ").append(tempPath).toString();
	FileWatcher fw = new FileWatcher(tempPath);
	fw.start();
	runspace.invoke(cmd, session.getTimeout(IWindowsSession.Timeout.XL));
	fw.interrupt();
	runspace.invoke("Gzip-File " + tempPath);
	return fs.getFile(tempPath + ".gz", IFile.Flags.READWRITE);
    }

    class ReaderIterator implements Iterator<String> {
	BufferedReader reader;
	String next = null;

	ReaderIterator(BufferedReader reader) {
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

    /**
     * Periodically checks the length of a file, in a background thread. This gives us a clue as to whether very long
     * searches are really doing anything, or if they've died.
     */
    class FileWatcher implements Runnable {
	private String path;
	private Thread thread;
	private boolean cancel = false;

	FileWatcher(String path) {
	    this.path = path;
	}

	void start() {
	    thread = new Thread(this);
	    thread.start();
	}

	void interrupt() {
	    cancel = true;
	    thread.interrupt();
	}

	// Implement Runnable

	public void run() {
	    while(!cancel) {
		try {
		    Thread.sleep(15000);
		    IFile f = fs.getFile(path, IFile.Flags.READVOLATILE);
		    if (f.exists()) {
			logger.info(JOVALMsg.STATUS_FS_SEARCH_CACHE_PROGRESS, f.length());
		    } else {
			cancel = true;
		    }
	        } catch (IOException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    cancel = true;
	        } catch (InterruptedException e) {
		    cancel = true;
	        }
	    }
	}
    }

    static class InternalACE implements IACE {
	private int flags, mask;
	private String sid;

	public InternalACE(String s) throws IllegalArgumentException {
	    int begin = s.indexOf("mask=") + 5;
	    int end = s.indexOf(",sid=");
	    if (begin < 0 || end < 0) {
		throw new IllegalArgumentException(s);
	    } else {
		mask = Integer.parseInt(s.substring(begin, end));
		begin = end + 5;
		sid = s.substring(begin);
	    }
	}

	public InternalACE(int mask, String sid) {
	    this.mask = mask;
	    this.sid = sid;
	}

	public int getFlags() {
	    return 0;
	}

	public int getAccessMask() {
	    return mask;
	}

	public String getSid() {
	    return sid;
	}
    }

    /**
     * JDBM Serilizer implementation for Windows IFiles
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
		break;
	    }
	    long len = in.readLong();
	    int winType = in.readInt();
	    IACE[] aces = new IACE[in.readInt()];
	    for (int i=0; i < aces.length; i++) {
		aces[i] = new InternalACE(in.readInt(), in.readUTF());
	    }
	    WindowsFileInfo info = new WindowsFileInfo(ctime, mtime, atime, type, len, winType, aces);
	    if (fs == null) {
		fs = WindowsFileSearcher.fsInstances.get(instanceKey);
	    }
	    return fs.createFileFromInfo(path, info);
	}

	public void serialize(DataOutput out, IFile f) throws IOException {
	    out.writeUTF(f.getPath());
	    out.writeLong(f.createTime());
	    out.writeLong(f.lastModified());
	    out.writeLong(f.accessTime());
	    if (f.isLink()) {
		out.writeInt(SER_LINK);
	    } else if (f.isDirectory()) {
		out.writeInt(SER_DIRECTORY);
	    } else {
		out.writeInt(SER_FILE);
	    }
	    out.writeLong(f.length());
	    IWindowsFileInfo info = (IWindowsFileInfo)f.getExtended();
	    out.writeInt(info.getWindowsFileType());
	    IACE[] aces = info.getSecurity();
	    if (aces == null) {
		out.writeInt(0);
	    } else {
		out.writeInt(aces.length);
		for (IACE ace : aces) {
		    out.writeInt(ace.getAccessMask());
		    out.writeUTF(ace.getSid());
		}
	    }
	}
    }
}
