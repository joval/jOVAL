// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ILoggable;
import org.joval.intf.util.ISearchable;
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
    private IUnixSession session;
    private IUnixFilesystemDriver driver;
    private LocLogger logger;

    public UnixFileSearcher(IUnixSession session, IUnixFilesystemDriver driver) {
	this.session = session;
	this.driver = driver;
	logger = session.getLogger();
    }

    // Implement ILogger

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ISearchable.ISearchPlugin<IFile>

    public String getSubcommand() {
	return driver.getStatCommand();
    }

    public IFile createObject(Iterator<String> input) {
	UnixFilesystem.UnixFileInfo info = (UnixFilesystem.UnixFileInfo)driver.nextFileInfo(input);
	if (info == null) {
	    return null;
	} else if (info.getPath() == null) {
	    //
	    // Skip a bad entry and try again
	    //
	    return createObject(input);
	} else {
	    return ((UnixFilesystem)session.getFilesystem()).new UnixFile(info);
	}
    }

    // Implement ISearchable<IFile>

    public Collection<IFile> search(String from, Pattern p, int maxDepth, int flags, ISearchPlugin<IFile> plugin)
		throws Exception {

	Collection<IFile> results = new ArrayList<IFile>();
	File localTemp = null;
	IFile remoteTemp = null;
	try {
	    String command = driver.getFindCommand(from, maxDepth, flags, p == null ? null : p.pattern());

	    //
	    // Run the command on the remote host, storing the results in a temporary file, then tranfer the file
	    // locally and read it.
	    //
	    IReader reader = null;
	    remoteTemp = execToFile(command);
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
		    if (remoteTemp.exists()) {
			SafeCLI.exec("rm -f " + remoteTemp.getPath(), session, IUnixSession.Timeout.S);
		    }
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return results;
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
	    long len = session.getFilesystem().getFile(tempPath, IFile.Flags.READVOLATILE).length();
	    logger.info(JOVALMsg.STATUS_FS_SEARCH_CACHE_PROGRESS, len);
	}
	if (!done) {
	    p.destroy();
	}
	er1.close();
	er2.close();
	er1.join();
	er2.join();

	return session.getFilesystem().getFile(tempPath, IFile.Flags.READWRITE);
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
}
