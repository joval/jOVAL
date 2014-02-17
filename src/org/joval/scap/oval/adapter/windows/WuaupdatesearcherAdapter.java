// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.WuaUpdateSearcherBehaviors;
import scap.oval.definitions.windows.WuaupdatesearcherObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.WuaupdatesearcherItem;

import org.joval.intf.plugin.IAdapter;
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
    private HashSet<String> runspaceIds;
    private Map<String, WuaupdatesearcherItem> itemCache;
    private Map<String, MessageType> errors;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    runspaceIds = new HashSet<String>();
	    classes.add(WuaupdatesearcherObject.class);
	    itemCache = new HashMap<String, WuaupdatesearcherItem>();
	    errors = new HashMap<String, MessageType>();
	} else {
	    notapplicable.add(WuaupdatesearcherObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	WuaupdatesearcherObject wObj = (WuaupdatesearcherObject)obj;
	boolean includeSuperseded = false;
	if (wObj.isSetBehaviors()) {
	    WuaUpdateSearcherBehaviors behaviors = wObj.getBehaviors();
	    includeSuperseded = behaviors.getIncludeSupersededUpdates();
	}

	Collection<WuaupdatesearcherItem> items = new ArrayList<WuaupdatesearcherItem>();
	OperationEnumeration op = wObj.getSearchCriteria().getOperation();
	switch(op) {
	  case EQUALS:
	    String searchCriteria = SafeCLI.checkArgument((String)wObj.getSearchCriteria().getValue(), session);
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
	    long timeout = session.getTimeout(IWindowsSession.Timeout.L);
	    data = getRunspace().invoke("Get-WuaUpdates \"" + searchString + "\" " + supersededArg, timeout);
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
     * Get or create a runspace with the searcher interface module
     */
    private IRunspace getRunspace() throws Exception {
	IRunspace runspace = session.getRunspacePool().getRunspace();
	if (!runspaceIds.contains(runspace.getId())) {
	    runspace.loadModule(getClass().getResourceAsStream("Wuaupdatesearcher.psm1"));
	    runspaceIds.add(runspace.getId());
	}
	return runspace;
    }
}
