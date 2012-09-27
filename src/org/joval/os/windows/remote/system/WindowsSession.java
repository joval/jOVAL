// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.system;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.slf4j.cal10n.LocLogger;

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
import org.joval.intf.util.IPathRedirector;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IWindowsCredential;
import org.joval.intf.windows.io.IWindowsFilesystem;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.registry.IStringValue;
import org.joval.intf.windows.registry.IValue;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.intf.ws.IPort;
import org.joval.os.windows.identity.Directory;
import org.joval.os.windows.io.WOW3264FilesystemRedirector;
import org.joval.os.windows.registry.WOW3264RegistryRedirector;
import org.joval.os.windows.remote.io.SmbFilesystem;
import org.joval.os.windows.remote.powershell.RunspacePool;
import org.joval.os.windows.remote.registry.Registry;
import org.joval.os.windows.remote.wmi.WmiConnection;
import org.joval.os.windows.remote.wmi.WmiProcessControl;
import org.joval.os.windows.remote.winrm.Shell;
import org.joval.os.windows.remote.wsmv.WSMVPort;
import org.joval.util.AbstractSession;
import org.joval.util.Checksum;
import org.joval.util.CachingHierarchy;
import org.joval.util.Environment;
import org.joval.util.JOVALMsg;

/**
 * @author David A. Solin
 * @version %I% %G%
 */
public class WindowsSession extends AbstractSession implements IWindowsSession, ILocked {
    private static int counter = 0;
    static {
	JISystem.getLogger().setLevel(Level.WARNING);
	JISystem.setAutoRegisteration(true);
	JISystem.setJavaCoClassAutoCollection(false);
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private String host;
    private String tempDir=null, cwd;
    private IWindowsCredential cred;
    private Registry reg, reg32;
    private IWindowsFilesystem fs32;
    private boolean is64bit = false;
    private RunspacePool runspaces = null;
    private Directory directory = null;
    private WmiConnection conn;
    private WSMVPort port;
    private FileOutputStream soapLog;

    public WindowsSession(String host, File wsdir) {
	super();
	this.wsdir = wsdir;
	this.host = host;
    }

    // Implement IWindowsSession extensions

    public IRunspacePool getRunspacePool() {
	if (runspaces == null) {
	    try {
		runspaces = new RunspacePool(new Shell(port, env, cwd), port, logger);
	    } catch (Exception e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return runspaces;
    }

    public IDirectory getDirectory() {
	return directory;
    }

    public IRegistry getRegistry(View view) {
	switch(view) {
	  case _32BIT:
	    return reg32;
	}
	return reg;
    }

    public boolean supports(View view) {
	switch(view) {
	  case _32BIT:
	    return true;
	  case _64BIT:
	  default:
	    return is64bit;
	}
    }

    public IWindowsFilesystem getFilesystem(View view) {
	switch(view) {
	  case _32BIT:
	    return fs32;
	}
	return (IWindowsFilesystem)fs;
    }

    public IWmiProvider getWmiProvider() {
	return conn;
    }

    // Implement ILoggable

    @Override
    public void setLogger(LocLogger logger) {
	super.setLogger(logger);
	if (fs32 != null && !fs32.equals(fs)) {
	    fs32.setLogger(logger);
	}
	if (conn != null) {
	    conn.setLogger(logger);
	}
	if (directory != null) {
	    directory.setLogger(logger);
	}
	if (port != null) {
	    port.setLogger(logger);
	}
    }

    // Implement ILocked

    public boolean unlock(ICredential credential) {
	if (credential instanceof IWindowsCredential) {
	    cred = (IWindowsCredential)credential;
	    return true;
	} else {
	    return false;
	}
    }

    // Implement IBaseSession

    @Override
    public synchronized void dispose() {
	super.dispose();
	if (fs32 instanceof CachingHierarchy) {
	    ((CachingHierarchy)fs32).dispose();
	}
    }

    @Override
    public String getUsername() {
	return cred.getUsername();
    }

    @Override
    public void setWorkingDir(String path) {
	cwd = env.expand(path);
    }

    @Override
    public long getTime() throws Exception {
	ISWbemObjectSet results = conn.execQuery(IWmiProvider.CIMv2, "select * from Win32_UTCTime");
	ISWbemPropertySet props = results.iterator().next().getProperties();
	StringBuffer sb = new StringBuffer();
	sb.append(props.getItem("Year").getValueAsString());
	sb.append("-");
	sb.append(props.getItem("Month").getValueAsString());
	sb.append("-");
	sb.append(props.getItem("Day").getValueAsString());
	sb.append(" ");
	sb.append(props.getItem("Hour").getValueAsString());
	sb.append(":");
	sb.append(props.getItem("Minute").getValueAsString());
	sb.append(":");
	sb.append(props.getItem("Second").getValueAsString());
	sb.append(" +0000");
	synchronized(sdf) {
	    return sdf.parse(sb.toString()).getTime();
	}
    }

    @Override
    public IProcess createProcess(String command, String[] env) throws Exception {
	String method = getProperties().getProperty(PROP_REMOTE_EXEC_IMPL);
	if (VAL_WINRM.equals(method)) {
	    Environment environment = new Environment(this.env);
	    if (env != null) {
		for (String pair : env) {
		    environment.setenv(pair);
		}
	    }
	    return new Shell(port, environment, cwd).createProcess(command);
	} else {
	    //
	    // WMI process control is the default
	    //
	    StringBuffer sb = new StringBuffer(getTempDir()).append(IWindowsFilesystem.DELIM_STR).append("rexec_");
	    sb.append(Integer.toHexString(counter++));

	    IFile out = fs.getFile(sb.toString() + ".out", IFile.READVOLATILE);
	    out.getOutputStream(false).close(); // create/clear tmpOutFile
	    deleteOnDisconnect(out);

	    IFile err = fs.getFile(sb.toString() + ".err", IFile.READVOLATILE);
	    err.getOutputStream(false).close(); // create/clear tmpErrFile
	    deleteOnDisconnect(err);

	    WmiProcessControl wp = new WmiProcessControl(conn, this.env, command, env, cwd, out, err);
	    wp.setLogger(logger);
	    return wp;
	}
    }

    public String getHostname() {
	if (isConnected()) {
	    try {
		IKey key = reg.fetchKey(IRegistry.HKLM, IRegistry.COMPUTERNAME_KEY);
		IValue val = key.getValue(IRegistry.COMPUTERNAME_VAL);
		if (val.getType() == IValue.REG_SZ) {
		    return ((IStringValue)val).getData();
		} else {
		    logger.warn(JOVALMsg.ERROR_SYSINFO_HOSTNAME);
		}
	    } catch (Exception e) {
		logger.warn(JOVALMsg.ERROR_SYSINFO_HOSTNAME);
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return host;
    }

    public synchronized boolean connect() {
	if (cred == null) {
	    return false;
	} else {
	    if (reg == null) {
		reg = new Registry(host, cred, null, this);
	    }
	    if (reg.connect()) {
		if (env == null) {
		    env = reg.getEnvironment();
		}
		if (fs == null) {
		    fs = new SmbFilesystem(this, cred, env, null, "winfs.db");
		}
		is64bit = env.getenv(ENV_ARCH).indexOf("64") != -1;
		if (is64bit) {
		    WOW3264RegistryRedirector.Flavor flavor = WOW3264RegistryRedirector.getFlavor(reg);
		    reg32 = new Registry(host, cred, new WOW3264RegistryRedirector(flavor), this);
		    if (!reg32.connect()) {
			reg.disconnect();
			return false;
		    }
		    if (fs32 == null) {
			fs32 = new SmbFilesystem(this, cred, env, new WOW3264FilesystemRedirector(env), "winfs32.db");
		    }
		} else {
		    reg32 = reg;
		    fs32 = (IWindowsFilesystem)fs;
		}
		try {
		    getTempDir();
		} catch (IOException e) {
		    return false;
		}
		cwd = env.expand("%SystemRoot%");
		if (port == null) {
		    StringBuffer sb = new StringBuffer("http://").append(host).append(":5985/wsman");
		    try {
			port = new WSMVPort(sb.toString(), null, cred);
			port.setLogger(logger);
			port.setEncryption(getProperties().getBooleanProperty(IPort.PROP_ENCRYPT));
			if (getProperties().getBooleanProperty(IPort.PROP_DEBUG)) {
			    soapLog = new FileOutputStream(new File(wsdir, "wsmv.soap.log"));
			    port.setDebug(soapLog);
			}
		    } catch (Exception e) {
			logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			reg.disconnect();
			if (is64bit) {
			    reg32.disconnect();
			}
			return false;
		    }
		}
		if (conn == null) {
		    conn = new WmiConnection(host, cred, this);
		}
		if (conn.connect()) {
		    connected = true;
		    if (directory == null) {
			directory = new Directory(this);
		    }
		    directory.setWmiProvider(conn);
		    return true;
		} else {
		    reg.disconnect();
		    if (is64bit) {
			reg32.disconnect();
		    }
		    return false;
		}
	    } else {
		return false;
	    }
	}
    }

    public synchronized void disconnect() {
	try {
	    deleteFiles();
	    if (runspaces != null) {
		runspaces.shutdown();
	    }
	    if (soapLog != null) {
		try {
		    soapLog.close();
		} catch (IOException e) {
		}
	    }
	    reg.disconnect();
	    if (is64bit) {
		reg32.disconnect();
	    }
	    if (conn != null) {
		conn.disconnect();
	    }
	} finally {
	    connected = false;
	}
    }

    public Type getType() {
	return Type.WINDOWS;
    }

    // Implement ISession

    @Override
    public String getTempDir() throws IOException {
	if (tempDir == null) {
	    Iterator<String> iter = getTempDirCandidates().iterator();
	    while(iter.hasNext()) {
		String path = iter.next();
		if (testDir(path)) {
		    tempDir = path;
		    break;
		}
	    }
	}
	return tempDir;
    }

    // Private

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
		return fs.getFile(path).isDirectory();
	    } catch (Exception e) {
	    }
	}
	return false;
    }

    private String getChecksum(String[] env) {
	StringBuffer sb = new StringBuffer("");
	if (env != null) {
	    for (String s : env) {
		if (sb.length() > 0) {
		    sb.append("\n");
		}
		sb.append(s);
	    }
	}
	return Checksum.getChecksum(sb.toString(), Checksum.Algorithm.MD5);
    }
}
