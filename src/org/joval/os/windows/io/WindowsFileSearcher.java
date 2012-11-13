// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
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
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An interface for searching a Windows filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileSearcher implements ISearchable<IFile>, ISearchable.ISearchPlugin<IFile>, ILoggable {
    private IWindowsSession session;
    private IRunspace runspace;
    private LocLogger logger;
    private HashMap<String, Collection<IFile>> searchMap;

    public WindowsFileSearcher(IWindowsSession session) throws Exception {
	this(session, null);
    }

    public WindowsFileSearcher(IWindowsSession session, IWindowsSession.View view) throws Exception {
	this.session = session;
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
	searchMap = new HashMap<String, Collection<IFile>>();
    }

    // Implement ILogger

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ISearchable<IFile>

    /**
     * Recursively search for elements matching the given pattern.
     *
     * @param from starting point for the search (search happens below this path)
     * @param maxDepth the maximum number of hierarchies to traverse while searching. DEPTH_UNLIMITED for unlimited.
     * @param flags application-specific flags.
     * @param plugin @see ISearchable.ISearchPlugin
     */
    public Collection<IFile> search(String from, Pattern p, int maxDepth, int flags, ISearchPlugin<IFile> plugin)
		throws Exception {

	logger.debug(JOVALMsg.STATUS_FS_SEARCH_START, p == null ? "null" : p.pattern(), from, flags);
	String cmd = getFindCommand(from, maxDepth, flags, p == null ? null : p.pattern());
	if (searchMap.containsKey(cmd)) {
	    return searchMap.get(cmd);
	} else {
	    logger.debug(JOVALMsg.STATUS_FS_SEARCH_START, p == null ? "null" : p.pattern(), from, flags);
	    File localTemp = null;
	    IFile remoteTemp = null;
	    Collection<IFile> results = new ArrayList<IFile>();
	    try {
		//
		// Run the command on the remote host, storing the results in a temporary file, then tranfer the file
		// locally and read it.
		//
		BufferedReader reader = null;
		remoteTemp = execToFile(cmd);
		if(session.getWorkspace() == null) {
		    //
		    // State cannot be saved locally, so the result will have to be buffered into memory
		    //
		    reader = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(remoteTemp.getInputStream()), StringTools.UTF16LE));
		} else {
		    //
		    // Read from the local state file, or create one while reading from the remote state file.
		    //
		    localTemp = File.createTempFile("search", null, session.getWorkspace());
		    StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(localTemp), true);
		    reader = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(new FileInputStream(localTemp)), StringTools.UTF16LE));
		}

		IFile file = null;
		Iterator<String> iter = new ReaderIterator(reader);
		while ((file = plugin.createObject(iter)) != null) {
		    logger.debug(JOVALMsg.STATUS_FS_SEARCH_MATCH, file.getPath());
		    results.add(file);
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
		    } catch (Exception e) {
			logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	    searchMap.put(cmd, results);
	    return results;
	}
    }

    // Implement ISearchPlugin<IFile>

    public String getSubcommand() {
	return "| Print-FileInfo";
    }

    static final String START	= "{";
    static final String END	= "}";

    public IFile createObject(Iterator<String> input) {
	IFile file = null;
	boolean start = false;
	while(input.hasNext()) {
	    if (input.next().trim().equals(START)) {
		start = true;
		break;
	    }
	}
	if (start) {
	    long ctime=IFile.UNKNOWN_TIME, mtime=IFile.UNKNOWN_TIME, atime=IFile.UNKNOWN_TIME, length=-1L;
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
				    length = Long.parseLong(val);
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
	    WindowsFileInfo info = new WindowsFileInfo(ctime, mtime, atime, type, length, winType, aces.toArray(new IACE[0]));
	    file = ((AbstractFilesystem)session.getFilesystem()).createFileFromInfo(path, info);
	}
	return file;
    }

    // Internal

    String getFindCommand(String from, int maxDepth, int flags, String pattern) {
	StringBuffer command;
	switch(flags) {
	  case ISearchable.FLAG_CONTAINERS:
	    command = new StringBuffer("Find-Directories");
	    break;
	  default:
	    command = new StringBuffer("Find-Files");
	    break;
	}
	command.append(" -Path \"");
	command.append(from);
	command.append("\" -Depth ");
	command.append(Integer.toString(maxDepth));
	if (pattern != null) {
	    command.append(" -Pattern \"");
	    command.append(pattern);
	    command.append("\"");
	}
	command.append(" ");
	command.append(getSubcommand());
	return command.toString();
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
	logger.info(JOVALMsg.STATUS_FS_SEARCH_CACHE_TEMP, tempPath);

	String cmd = new StringBuffer(command).append(" | Out-File ").append(tempPath).toString();
	FileWatcher fw = new FileWatcher(tempPath);
	fw.start();
	runspace.invoke(cmd);
	fw.interrupt();
	runspace.invoke("Gzip-File " + tempPath);
	return session.getFilesystem().getFile(tempPath + ".gz", IFile.Flags.READWRITE);
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
		    IFile f = session.getFilesystem().getFile(path, IFile.Flags.READVOLATILE);
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

    class InternalACE implements IACE {
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
}
