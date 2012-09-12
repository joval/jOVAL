// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.powershell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.HashMap;

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
import org.joval.util.SessionException;

/**
 * An implementation of a remote runspace pool based on a generic ISession and an IWSMVPort.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunspacePool implements IRunspacePool, IWSMVConstants {
    private Shell shell;
    private IPort port;
    private HashMap<String, Runspace> pool;
    private int capacity;

    public RunspacePool(Shell shell, IPort port) throws Exception {
	this.shell = shell;
	this.port = port;

	pool = new HashMap<String, Runspace>();

	//
	// Set runspace capacity to the maximum number of concurrent operations per user
	//
	AnyXmlOptionalType arg = Factories.TRANSFER.createAnyXmlOptionalType();
	GetOperation operation = new GetOperation(arg);
	operation.addResourceURI(CONFIG_URI);
	AnyXmlType any = (AnyXmlType)operation.dispatch(port);
	ConfigType config = (ConfigType)any.getAny();
	ServiceType service = config.getService();
	capacity = service.getMaxConcurrentOperationsPerUser().intValue();
    }

    public void shutdown() {
	if (shell != null) {
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

    public Collection<IRunspace> enumerate() {
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

    public IRunspace spawn() throws Exception {
	if (pool.size() < capacity()) {
	    String id = Integer.toString(pool.size());
	    Runspace runspace = new Runspace(id, shell.createProcess("powershell -File -", false));
	    pool.put(id, runspace);
	    return runspace;
	} else {
	    throw new IndexOutOfBoundsException(Integer.toString(pool.size() + 1));
	}
    }
}
