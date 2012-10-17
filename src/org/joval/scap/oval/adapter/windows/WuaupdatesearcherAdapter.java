// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.WuaUpdateSearcherBehaviors;
import oval.schemas.definitions.windows.WuaupdatesearcherObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.WuaupdatesearcherItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:wuaupdatesearcher_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WuaupdatesearcherAdapter implements IAdapter {
    private IWindowsSession session;
    private IRunspace runspace;
    private Hashtable<String, WuaupdatesearcherItem> itemCache;
    private Hashtable<String, MessageType> errors;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(WuaupdatesearcherObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (itemCache == null) {
	    init();
	}
	if (runspace == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL), FlagEnumeration.NOT_COLLECTED);
	}
	WuaupdatesearcherObject wObj = (WuaupdatesearcherObject)obj;
	boolean includeSuperseded = false;
	if (wObj.isSetBehaviors()) {
	    WuaUpdateSearcherBehaviors behaviors = wObj.getBehaviors();
	    includeSuperseded = behaviors.getIncludeSupersededUpdates();
	}

	Collection<WuaupdatesearcherItem> items = new Vector<WuaupdatesearcherItem>();
	String searchCriteria = (String)wObj.getSearchCriteria().getValue();
	OperationEnumeration op = wObj.getSearchCriteria().getOperation();
	switch(op) {
	  case EQUALS:
	    String key = searchCriteria + includeSuperseded;
	    if (!itemCache.containsKey(key)) {
		WuaupdatesearcherItem item = makeItem(searchCriteria, includeSuperseded);
		itemCache.put(key, item);
	    }
	    items.add(itemCache.get(key));
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    // Private

    private WuaupdatesearcherItem makeItem(String searchString, boolean includeSuperseded) {
	WuaupdatesearcherItem item = Factories.sc.windows.createWuaupdatesearcherItem();
	EntityItemStringType searchType = Factories.sc.core.createEntityItemStringType();
	searchType.setValue(searchString);
	item.setSearchCriteria(searchType);
	session.getLogger().debug(JOVALMsg.STATUS_WIN_WUA, searchString);
	String data = null;
	try {
	    String supersededArg = includeSuperseded ? "1" : "0";
	    data = runspace.invoke("Get-WuaUpdates \"" + searchString + "\" " + supersededArg);
	} catch (Exception e) {
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_WUA_SEARCH, searchString, e.getMessage());
	    session.getLogger().warn(s);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(s);
	    item.getMessage().add(msg);
	    item.setStatus(StatusEnumeration.ERROR);
	}
	if (data != null) {
	    HashSet<String> updateIds = new HashSet<String>();
	    for (String line : data.split("\n")) {
		updateIds.add(line.trim());
	    }
	    for (String updateId : updateIds) {
		EntityItemStringType updateType = Factories.sc.core.createEntityItemStringType();
		updateType.setValue(updateId);
		item.getUpdateId().add(updateType);
	    }
	}
	return item;
    }

    /**
     * Initialize the adapter and install the probe on the target host.
     */
    private void init() {
	itemCache = new Hashtable<String, WuaupdatesearcherItem>();
	errors = new Hashtable<String, MessageType>();

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Get-AccessTokens
	// Powershell module code.
	//
	for (IRunspace rs : session.getRunspacePool().enumerate()) {
	    runspace = rs;
	    break;
	}
	try {
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn();
	    }
	    if (runspace != null) {
		runspace.loadModule(getClass().getResourceAsStream("Wuaupdatesearcher.psm1"));
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }
}
