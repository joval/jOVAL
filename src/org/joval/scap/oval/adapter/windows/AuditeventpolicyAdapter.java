// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.util.IniFile;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.AuditeventpolicyObject;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.AuditeventpolicyItem;
import scap.oval.systemcharacteristics.windows.EntityItemAuditType;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves the unary windows:passwordpolicy_item.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AuditeventpolicyAdapter implements IAdapter {
    private IWindowsSession session;
    private HashSet<String> runspaceIds;
    private Collection<AuditeventpolicyItem> items = null;
    private CollectException error = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    runspaceIds = new HashSet<String>();
	    classes.add(AuditeventpolicyObject.class);
	} else {
	    notapplicable.add(AuditeventpolicyObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw error;
	} else if (items == null) {
	    items = makeItems();
	}
	return items;
    }

    // Private

    private Collection<AuditeventpolicyItem> makeItems() throws CollectException {
	try {
	    IRunspace runspace = session.getRunspacePool().getRunspace();
	    if (!runspaceIds.contains(runspace.getId())) {
		runspace.loadAssembly(getClass().getResourceAsStream("Auditeventpolicy.dll"));
		runspace.loadModule(getClass().getResourceAsStream("Auditeventpolicy.psm1"));
		runspaceIds.add(runspace.getId());
	    }
	    AuditeventpolicyItem item = Factories.sc.windows.createAuditeventpolicyItem();
	    for (String line : runspace.invoke("Get-AuditEventPolicies").split("\r\n")) {
		int ptr = line.indexOf(":");
		if (ptr != -1) {
		    String key = line.substring(0,ptr);
		    String val = line.substring(ptr+1).trim();
		    if ("ACCOUNT_LOGON".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setAccountLogon(type);
		    } else if ("ACCOUNT_MANAGEMENT".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setAccountManagement(type);
		    } else if ("DETAILED_TRACKING".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setDetailedTracking(type);
		    } else if ("DIRECTORY_SERVICE_ACCESS".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setDirectoryServiceAccess(type);
		    } else if ("LOGON".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setLogon(type);
		    } else if ("OBJECT_ACCESS".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setObjectAccess(type);
		    } else if ("POLICY_CHANGE".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setPolicyChange(type);
		    } else if ("PRIVILEGE_USE".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setPrivilegeUse(type);
		    } else if ("SYSTEM".equals(key)) {
			EntityItemAuditType type = Factories.sc.windows.createEntityItemAuditType();
			type.setValue(val);
			item.setSystem(type);
		    }
		}
	    }
	    return Arrays.asList(item);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }
}
