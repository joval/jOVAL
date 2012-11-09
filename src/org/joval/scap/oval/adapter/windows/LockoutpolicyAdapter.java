// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.LockoutpolicyObject;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.LockoutpolicyItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves the unary windows:lockoutpolicy_item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LockoutpolicyAdapter implements IAdapter {
    private IWindowsSession session;
    private String runspaceId;
    private Collection<LockoutpolicyItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(LockoutpolicyObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (items == null) {
	    makeItem();
	}
	return items;
    }

    // Private

    private void makeItem() throws CollectException {
	try {
	    //
	    // Get a runspace if there are any in the pool, or create a new one, and load the Get-LockoutPolicy
	    // Powershell module code.
	    //
	    IRunspace runspace = null;
	    for (IRunspace rs : session.getRunspacePool().enumerate()) {
		runspace = rs;
		break;
	    }
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn();
	    }
	    runspace.loadModule(getClass().getResourceAsStream("Lockoutpolicy.psm1"));

	    //
	    // Run the Get-LockoutPolicy module and parse the output
	    //
	    LockoutpolicyItem item = Factories.sc.windows.createLockoutpolicyItem();
	    for (String line : runspace.invoke("Get-LockoutPolicy").split("\n")) {
		line = line.trim();
		int ptr = line.indexOf("=");
		String key=null, val=null;
		if (ptr > 0) {
		    key = line.substring(0,ptr);
		    val = line.substring(ptr+1);
		}
		try {
		    if ("force_logoff".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new Integer(val).toString());
			item.setForceLogoff(type);
		    } else if ("lockout_duration".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new Integer(val).toString());
			item.setLockoutDuration(type);
		    } else if ("lockout_observation_window".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new Integer(val).toString());
			item.setLockoutObservationWindow(type);
		    } else if ("lockout_threshold".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new Integer(val).toString());
			item.setLockoutThreshold(type);
		    }
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_WIN_LOCKOUTPOLICY_VALUE, e.getMessage(), key);
		}
	    }
	    items = new Vector<LockoutpolicyItem>();
	    items.add(item);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
