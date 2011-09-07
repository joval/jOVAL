// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.system;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.jinterop.dcom.common.JISystem;

import com.h9labs.jwbem.SWbemLocator;
import com.h9labs.jwbem.SWbemServices;

import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.identity.WindowsCredential;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
import org.joval.os.windows.remote.io.SmbFilesystem;
import org.joval.os.windows.remote.registry.Registry;
import org.joval.os.windows.remote.wmi.WmiConnection;
import org.joval.util.JOVALSystem;

/**
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSession implements IWindowsSession, ILocked {
    private static int counter = 0;
    static {
	JISystem.getLogger().setLevel(Level.WARNING);
	JISystem.setAutoRegisteration(true);
	JISystem.setJavaCoClassAutoCollection(false);
    }

    private String host;
    private String tempDir, cwd;
    private WindowsCredential cred;
    private WmiConnection conn;
    private IEnvironment env;
    private Registry registry;
    private SmbFilesystem fs;
    private Vector<IFile> tempFiles;
    private boolean redirect64 = true;

    public WindowsSession(String host) {
	this.host = host;
	tempFiles = new Vector<IFile>();
    }

    // Implement IWindowsSession extensions

    public void set64BitRedirect(boolean redirect64) {
	this.redirect64 = redirect64;
    }

    public IRegistry getRegistry() {
	return registry;
    }

    public IWmiProvider getWmiProvider() {
	return conn;
    }

    // Implement ILocked

    public boolean unlock(ICredential credential) {
	if (credential instanceof WindowsCredential) {
	    cred = (WindowsCredential)credential;
	    return true;
	} else {
	    return false;
	}
    }

    // Implement ISession

    public boolean connect() {
	if (cred == null) {
	    return false;
	} else {
	    registry = new Registry(host, cred);
	    if (registry.connect()) {
		WOW3264RegistryRedirector.Flavor flavor = WOW3264RegistryRedirector.getFlavor(registry);
		registry.setRedirector(new WOW3264RegistryRedirector(redirect64, flavor));
		env = registry.getEnvironment();
		registry.disconnect();
		fs = new SmbFilesystem(host, cred, env);
		fs.set64BitRedirect(redirect64);
		try {
		    tempDir = getTempDir();
		} catch (IOException e) {
		    return false;
		}
		cwd = env.expand("%SystemRoot%");
		conn = new WmiConnection(host, cred);
		return conn.connect();
	    } else {
		return false;
	    }
	}
    }

    public void disconnect() {
	Iterator<IFile> iter = tempFiles.iterator();
	while(iter.hasNext()) {
	    IFile f = iter.next();
	    try {
		synchronized(f) {
		    if (f.exists()) {
			f.delete();
		    }
		}
	    } catch (Exception e) {
		JOVALSystem.getLogger().log(Level.WARNING, f.toString(), e);
	    }
	}
	conn.disconnect();
    }

    public void setWorkingDir(String path) {
	cwd = env.expand(path);
    }

    public Type getType() {
	return Type.WINDOWS;
    }

    public IEnvironment getEnvironment() {
	return env;
    }

    public IFilesystem getFilesystem() {
	return fs;
    }

    public IProcess createProcess(String command) throws Exception {
	StringBuffer sb = new StringBuffer(tempDir).append(fs.getDelimiter()).append("rexec_");
	sb.append(Integer.toHexString(counter++));

	IFile out = fs.getFile(sb.toString() + ".out", true);
	out.getOutputStream(false).close(); // create/clear tmpOutFile
	tempFiles.add(out);

	IFile err = fs.getFile(sb.toString() + ".err", true);
	err.getOutputStream(false).close(); // create/clear tmpErrFile
	tempFiles.add(err);

	WindowsProcess p = new WindowsProcess(conn.getServices(host, IWmiProvider.CIMv2), command, cwd, out, err);
	return p;
    }

    // Private

    private String getTempDir() throws IOException {
	Iterator<String> iter = getTempDirCandidates().iterator();
	while(iter.hasNext()) {
	    String path = iter.next();
	    if (testDir(path)) {
		return path;
	    }
	}
	throw new IOException("Unable to find a temp directory");
    }

    private List<String> getTempDirCandidates() {
	List<String> list = new Vector<String>();
	list.add("%TMP%");
	list.add("%TEMP%");
	list.add("%SystemDrive%\\Users\\" + cred.getUsername() + "\\AppData\\Local\\Temp");
	list.add("C:\\Users\\" + cred.getUsername() + "\\AppData\\Local\\Temp");
	list.add("%SystemRoot%\\Temp");
	return list;
    }

    private boolean testDir(String path) {
	if (path != null) {
	    try {
		IFile f = fs.getFile(path);
		return f.isDirectory();
	    } catch (Exception e) {
	    }
	}
	return false;
    }
}
