// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.LockoutpolicyObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.LockoutpolicyItem;

import org.joval.intf.plugin.IAdapter;
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
    private Collection<LockoutpolicyItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(LockoutpolicyObject.class);
	} else {
	    notapplicable.add(LockoutpolicyObject.class);
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
	    IWindowsSession.View view = session.getNativeView();
	    for (IRunspace rs : session.getRunspacePool().enumerate()) {
		if (rs.getView() == view) {
		    runspace = rs;
		    break;
		}
	    }
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn(view);
	    }
	    runspace.loadModule(getClass().getResourceAsStream("Lockoutpolicy.psm1"));

	    //
	    // Run the Get-LockoutPolicy module and parse the output
	    //
	    LockoutpolicyItem item = Factories.sc.windows.createLockoutpolicyItem();
	    String cmd = "Get-LockoutPolicy | Transfer-Encode";
	    for (String line : new String(Base64.decode(runspace.invoke(cmd)), StringTools.UTF8).split("\r\n")) {
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
			type.setValue(new BigInteger(val, 16).toString());
			item.setForceLogoff(type);
		    } else if ("lockout_duration".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new BigInteger(val, 16).toString());
			item.setLockoutDuration(type);
		    } else if ("lockout_observation_window".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new BigInteger(val, 16).toString());
			item.setLockoutObservationWindow(type);
		    } else if ("lockout_threshold".equals(key)) {
			EntityItemIntType type = Factories.sc.core.createEntityItemIntType();
			type.setDatatype(SimpleDatatypeEnumeration.INT.value());
			type.setValue(new BigInteger(val, 16).toString());
			item.setLockoutThreshold(type);
		    }
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_WIN_LOCKOUTPOLICY_VALUE, e.getMessage(), key);
		}
	    }
	    items = new ArrayList<LockoutpolicyItem>();
	    items.add(item);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
