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

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileMetadata;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.io.StreamTool;
import org.joval.io.fs.AbstractFilesystem;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;

/**
 * An interface for searching a Windows filesystem.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsFileSearcher implements ISearchable<IFile>, ILoggable {
    private IWindowsSession session;
    private AbstractFilesystem fs;
    private IRunspace runspace;
    private LocLogger logger;
    private Map<String, Collection<String>> searchMap;

    public WindowsFileSearcher(IWindowsSession session, IRunspace runspace, Map<String, Collection<String>> searchMap)
		throws Exception {

	this.session = session;
	this.searchMap = searchMap;
	logger = session.getLogger();
	this.runspace = runspace;
	runspace.loadModule(getClass().getResourceAsStream("WindowsFileSearcher.psm1"));
	fs = (AbstractFilesystem)session.getFilesystem(runspace.getView());
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

    public String[] guessParent(Pattern p, Object... args) {
	return fs.guessParent(p);
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
		results.add(fs.getFile(path));
	    }
	} else if (fs.getFile(from).isDirectory()) {
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
		if (session.getWorkspace() == null || IBaseSession.LOCALHOST.equals(session.getHostname())) {
		    in = new GZIPInputStream(remoteTemp.getInputStream());
		} else {
		    localTemp = File.createTempFile("search", null, session.getWorkspace());
		    StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(localTemp), true);
		    in = new GZIPInputStream(new FileInputStream(localTemp));
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StreamTool.detectEncoding(in)));
		Iterator<String> iter = new ReaderIterator(reader);
		IFile file = null;
		while ((file = createObject(iter)) != null) {
		    String path = file.getPath();
		    logger.debug(JOVALMsg.STATUS_FS_SEARCH_MATCH, path);
		    results.add(file);
		    paths.add(path);
		}
		logger.debug(JOVALMsg.STATUS_FS_SEARCH_DONE, results.size(), cmd);
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

    IFile createObject(Iterator<String> input) {
	WindowsFileInfo info = (WindowsFileInfo)((WindowsFilesystem)fs).nextFileInfo(input);
	if (info == null) {
	    return null;
	} else {
	    return fs.createFileFromInfo(info);
	}
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
	    if (thread.isAlive()) {
		thread.interrupt();
	    }
	}

	// Implement Runnable

	/**
	 * NB: This runs asynchronously during a search which takes place in Powershell, so we need to be careful not
	 *     to perform a file operation that requires the parent filesystem's Powershell instance (such as
	 *     IFile.isDirectory).
	 */
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
		    cancel = true;
	        } catch (InterruptedException e) {
		    cancel = true;
	        }
	    }
	}
    }
}
