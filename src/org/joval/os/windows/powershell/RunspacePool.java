// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.HashMap;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.powershell.IRunspacePool;

/**
 * An implementation of a runspace pool based on a generic ISession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunspacePool implements IRunspacePool {
    private ISession session;
    private HashMap<String, Runspace> pool;

    public RunspacePool(ISession session) throws IllegalArgumentException {
	if (session.getType() != ISession.Type.WINDOWS) {
	    throw new IllegalArgumentException(session.getType().toString());
	}
	this.session = session;
	pool = new HashMap<String, Runspace>();
    }

    public void shutdown() {
	for (Runspace runspace : pool.values()) {
	    try {
		runspace.println("exit");
		String line = null;
		long timeout = session.getTimeout(IBaseSession.Timeout.M);
		while((line = runspace.readLine(timeout)) != null) {}
		IProcess p = runspace.getProcess();
		p.waitFor(timeout);
		System.out.println("DAS: Runspace " + runspace.getId() + " exited with code " + p.exitValue());
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

    public Collection<IRunspace> enumerate() {
	Collection<IRunspace> runspaces = new ArrayList<IRunspace>();
	for (Runspace rs : pool.values()) {
	    runspaces.add(rs);
	}
	return runspaces;
    }

    public int capacity() {
	return 100;
    }

    public IRunspace get(String id) throws NoSuchElementException {
	if (pool.containsKey(id)) {
	    return pool.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public IRunspace spawn() throws Exception {
	if (pool.size() < capacity()) {
	    String id = Integer.toString(pool.size());
	    Runspace runspace = new Runspace(id, session.createProcess("powershell -File -", null));
	    pool.put(id, runspace);
	    return runspace;
	} else {
	    throw new IndexOutOfBoundsException(Integer.toString(pool.size() + 1));
	}
    }
}
