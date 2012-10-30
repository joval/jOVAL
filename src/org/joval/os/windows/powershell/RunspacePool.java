// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.HashMap;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.system.IProcess;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.powershell.IRunspacePool;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.util.JOVALMsg;

/**
 * An implementation of a runspace pool based on a generic IWindowsSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunspacePool implements IRunspacePool {
    private LocLogger logger;
    private IWindowsSession session;
    private HashMap<String, Runspace> pool;
    private int capacity;

    public RunspacePool(IWindowsSession session, int capacity) {
	this.session = session;
	logger = session.getLogger();
	this.capacity = capacity;
	pool = new HashMap<String, Runspace>();
    }

    public void shutdown() {
	for (Runspace runspace : pool.values()) {
	    try {
		runspace.invoke("exit");
		IProcess p = runspace.getProcess();
		p.waitFor(10000L);
		if (p.isRunning()) {
		    p.destroy();
		}
		logger.debug(JOVALMsg.STATUS_POWERSHELL_EXIT, runspace.getId());
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	pool.clear();
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
	    Runspace runspace = new Runspace(id, session.createProcess(Runspace.INIT_COMMAND, null), session.getLogger());
	    logger.debug(JOVALMsg.STATUS_POWERSHELL_SPAWN, id);
	    pool.put(id, runspace);
	    runspace.invoke("$host.UI.RawUI.BufferSize = New-Object System.Management.Automation.Host.Size(512,2000)");
	    return runspace;
	} else {
	    throw new IndexOutOfBoundsException(Integer.toString(pool.size() + 1));
	}
    }
}
