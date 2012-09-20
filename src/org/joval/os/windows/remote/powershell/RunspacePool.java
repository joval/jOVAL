// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.powershell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.TimerTask;

import org.slf4j.cal10n.LocLogger;

import com.microsoft.wsman.config.ConfigType;
import com.microsoft.wsman.config.ServiceType;
import org.xmlsoap.ws.transfer.AnyXmlOptionalType;
import org.xmlsoap.ws.transfer.AnyXmlType;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.wsmv.IWSMVConstants;
import org.joval.intf.ws.IPort;
import org.joval.os.windows.remote.winrm.Shell;
import org.joval.os.windows.remote.wsmv.operation.GetOperation;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SessionException;

/**
 * An implementation of a remote runspace pool based on a generic ISession and an IWSMVPort.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunspacePool implements IRunspacePool, IWSMVConstants {
    private static final long INTERVAL = 30000L;

    private LocLogger logger;
    private Shell shell;
    private IPort port;
    private HashMap<String, Runspace> pool;
    private int capacity;
    private RunspacePoolTask heartbeat;

    public RunspacePool(Shell shell, IPort port, LocLogger logger) throws Exception {
	this.shell = shell;
	this.port = port;
	this.logger = logger;

	pool = new HashMap<String, Runspace>();

	//
	// Set runspace capacity to the maximum number of concurrent operations per user, minus one
	//
	AnyXmlOptionalType arg = Factories.TRANSFER.createAnyXmlOptionalType();
	GetOperation operation = new GetOperation(arg);
	operation.addResourceURI(CONFIG_URI);
	AnyXmlType any = (AnyXmlType)operation.dispatch(port);
	ConfigType config = (ConfigType)any.getAny();
	ServiceType service = config.getService();
	capacity = service.getMaxConcurrentOperationsPerUser().intValue() - 1;

	heartbeat = new RunspacePoolTask();
	JOVALSystem.getTimer().scheduleAtFixedRate(heartbeat, INTERVAL, INTERVAL);
    }

    public void shutdown() {
	if (shell != null) {
	    heartbeat.cancel();
	    JOVALSystem.getTimer().purge();
	    for (Runspace runspace : pool.values()) {
		try {
		    runspace.invoke("exit");
		    IProcess p = runspace.getProcess();
		    p.waitFor(10000L);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    shell.dispose();
	    shell = null;
	    pool.clear();
	}
    }

    @Override
    protected void finalize() {
	shutdown();
    }

    // Implement IRunspacePool

    public synchronized Collection<IRunspace> enumerate() {
	Collection<IRunspace> runspaces = new ArrayList<IRunspace>();
	for (Runspace rs : pool.values()) {
	    runspaces.add(rs);
	}
	return runspaces;
    }

    public int capacity() {
	return capacity;
    }

    public IRunspace get(String id) throws NoSuchElementException {
	if (pool.containsKey(id)) {
	    return pool.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public synchronized IRunspace spawn() throws Exception {
	if (pool.size() < capacity()) {
	    String id = Integer.toString(pool.size());
	    Runspace runspace = new Runspace(id, shell.createProcess("powershell -NoProfile -File -", false));
	    pool.put(id, runspace);
	    runspace.invoke("$host.UI.RawUI.BufferSize = New-Object System.Management.Automation.Host.Size(512,2000)");
	    return runspace;
	} else {
	    throw new IndexOutOfBoundsException(Integer.toString(pool.size() + 1));
	}
    }

    // Internal

    class RunspacePoolTask extends TimerTask {
	RunspacePoolTask() {
	    super();
	}

	public void run() {
	    for (IRunspace ir : enumerate()) {
		try {
		    Runspace rs = (Runspace)ir;
		    long idle = System.currentTimeMillis() - rs.idleSince();
		    if (idle > 25000L) {
			rs.invoke("echo ping");
		    }
		} catch (Exception e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
    }
}
