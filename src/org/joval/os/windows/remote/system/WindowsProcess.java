// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import org.slf4j.cal10n.LocLogger;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.impls.automation.JIExcepInfo;

import com.h9labs.jwbem.SWbemServices;
import com.h9labs.jwbem.SWbemObjectSet;

import org.joval.intf.io.IFile;
import org.joval.intf.system.IProcess;
import org.joval.intf.util.ILoggable;
import org.joval.intf.windows.wmi.ISWbemEventSource;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.io.TailDashF;
import org.joval.os.windows.remote.wmi.WmiConnection;
import org.joval.os.windows.remote.wmi.scripting.SWbemSecurity;
import org.joval.os.windows.remote.wmi.win32.Win32Process;
import org.joval.os.windows.remote.wmi.win32.Win32ProcessStartup;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;

/**
 * Remote Windows implementation of an IProcess.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class WindowsProcess implements IProcess, ILoggable {
    private IWmiProvider wmi;
    private SWbemServices services;
    private Win32ProcessStartup startupInfo;
    private Win32Process process;
    private int pid = 0;
    private String command, cwd;
    private IFile out, err;
    private TailDashF outTail, errTail;
    private boolean running = false;
    private int exitCode = 0;
    private LocLogger logger;
    private Monitor monitor;

    WindowsProcess(WindowsSession ws, String command, String[] env, String cwd, IFile out, IFile err)
		throws JIException, UnknownHostException {

	wmi = ws.conn;
	services = ws.conn.getServices(ws.getHostname(), IWmiProvider.CIMv2);
	this.command = command;
	this.cwd = cwd;
	this.out = out;
	this.err = err;
	startupInfo = new Win32ProcessStartup(ws.getEnvironment(), services);
	startupInfo.setEnvironmentVariables(env);
	logger = JOVALMsg.getLogger();
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement IProcess

    public String getCommand() {
	return command;
    }

    // REMIND (DAS): implement this...
    public void setInteractive(boolean interactive) {
/*
	SWbemSecurity security = new SWbemSecurity(services);
	security.setImpersonationLevel(SWbemSecurity.ImpersonationLevel_IMPERSONATION);
*/
    }

    public void start() throws Exception {
	process = new Win32Process(services);
	outTail = new TailDashF(out);
	outTail.start();
	errTail = new TailDashF(err);
	errTail.start();
	StringBuffer sb = new StringBuffer("cmd /c ").append(command);
	sb.append(" >> ").append(out.getPath());
	sb.append(" 2>> ").append(err.getPath());
	int rc = process.create(sb.toString(), cwd, startupInfo);

	switch(rc) {
	  case Win32Process.SUCCESSFUL_COMPLETION:
	    pid = process.getProcessId();
	    running = true;
	    monitor = new Monitor();
	    monitor.start();
	    break;

	  default:
	    JIExcepInfo error = process.getError();
	    String code = Integer.toHexString(error.getErrorCode());
	    String description = error.getExcepDesc();
	    String source = error.getExcepSource();
	    throw new WmiException(JOVALMsg.getMessage(JOVALMsg.ERROR_WMI_PROCESS, rc, code, description, source));
	}
    }

    public InputStream getInputStream() {
	return outTail.getInputStream();
    }

    public InputStream getErrorStream() {
	return errTail.getInputStream();
    }

    public OutputStream getOutputStream() {
	throw new UnsupportedOperationException("Not implemented");
    }

    public void waitFor(long millis) throws InterruptedException {
	long end = Long.MAX_VALUE;
	if (millis > 0) {
	    end = System.currentTimeMillis() + millis;
	}
	while (isRunning() && System.currentTimeMillis() < end) {
	    Thread.sleep(Math.min(end - System.currentTimeMillis(), 250));
	}
    }

    public int exitValue() {
	return exitCode;
    }

    public boolean isRunning() {
	if (monitor != null) {
	    return monitor.isAlive();
	}
	return false;
    }

    public void destroy() {
	try {
	    process.terminate(1);
	    running = false;
	} catch (JIException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Private

    /**
     * The Monitor class waits for the process to finish for up to an hour, then it cleans up any open tails.
     */
    class Monitor implements Runnable {
	private Thread t;

	Monitor() {
	    t = new Thread(this);
	}

	void start() {
	    t.start();
	}

	boolean isAlive() {
	    return t.isAlive();
	}

	public void run() {
	    try {
		String wql = "select * from Win32_ProcessStopTrace where ProcessId=" + pid;
		ISWbemEventSource source = wmi.execNotificationQuery(IWmiProvider.CIMv2, wql);
		ISWbemObject event = source.nextEvent();
		ISWbemProperty exitStatus = event.getProperties().getItem("ExitStatus");
		if (exitStatus != null) {
		    exitCode = exitStatus.getValueAsInteger();
		}
	    } catch (WmiException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		String wql = "Select ProcessId from Win32_Process where ProcessId=" + pid;
		SWbemObjectSet set = services.execQuery(wql);
		if (set.getSize() > 0) {
		    destroy();
		}
	    }
	    try {
		synchronized(err) {
		    if (errTail != null) {
			if (errTail.isAlive()) {
			    errTail.interrupt();
			}
			err.delete();
		    }
		}
	    } catch (IOException e) {
		logger.warn(JOVALMsg.ERROR_IO, err.getPath(), e.getMessage());
	    }
	    try {
		synchronized(out) {
		    if (outTail != null) {
			if (outTail.isAlive()) {
			    outTail.interrupt();
			}
			out.delete();
		    }
		}
	    } catch (IOException e) {
		logger.warn(JOVALMsg.ERROR_IO, out.getPath(), e.getMessage());
	    }
	}
    }
}
