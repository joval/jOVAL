// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.SharedresourceObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemSharedResourceTypeType;
import scap.oval.systemcharacteristics.windows.SharedresourceItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Sharedresource objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SharedresourceAdapter implements IAdapter {
    public static final int ACCESS_READ		= 0x01;
    public static final int ACCESS_WRITE	= 0x02;
    public static final int ACCESS_CREATE	= 0x04;
    public static final int ACCESS_EXEC		= 0x08;
    public static final int ACCESS_DELETE	= 0x10;
    public static final int ACCESS_ATRIB	= 0x20;
    public static final int ACCESS_PERM		= 0x40;
    public static final int ACCESS_ALL		= 0x7F; // All of the above

    private IWindowsSession session;
    private Map<String, SharedresourceItem> shares;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(SharedresourceObject.class);
	} else {
	    notapplicable.add(SharedresourceObject.class);
	}
	return classes;
    }

    public Collection<SharedresourceItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	SharedresourceObject sObj = (SharedresourceObject)obj;
	OperationEnumeration op = sObj.getNetname().getOperation();
	Collection<SharedresourceItem> items = new ArrayList<SharedresourceItem>();
	switch(op) {
	  case EQUALS:
	  case CASE_INSENSITIVE_EQUALS:
	    for (Map.Entry<String, SharedresourceItem> entry : shares.entrySet()) {
		if (entry.getKey().equalsIgnoreCase((String)sObj.getNetname().getValue())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case NOT_EQUAL:
	  case CASE_INSENSITIVE_NOT_EQUAL:
	    for (Map.Entry<String, SharedresourceItem> entry : shares.entrySet()) {
		if (!entry.getKey().equalsIgnoreCase((String)sObj.getNetname().getValue())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = StringTools.pattern((String)sObj.getNetname().getValue());
		for (Map.Entry<String, SharedresourceItem> entry : shares.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue());
		    }
		}
	    } catch (PatternSyntaxException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (shares == null) {
	    try {
		IRunspace rs = session.getRunspacePool().getRunspace();
		rs.loadAssembly(getClass().getResourceAsStream("Sharedresource.dll"));
		rs.loadModule(getClass().getResourceAsStream("Sharedresource.psm1"));

		shares = new HashMap<String, SharedresourceItem>();
		byte[] data = Base64.decode(rs.invoke("Get-Shares | Transfer-Encode"));
		Iterator<String> lines = Arrays.asList(new String(data, StringTools.UTF8).split("\r\n")).iterator();
		SharedresourceItem item = null;
		while((item = nextSharedresourceItem(lines)) != null) {
		    shares.put((String)item.getNetname().getValue(), item);
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw error = new CollectException(e, FlagEnumeration.ERROR);
	    }
	}
    }

    private static final String OPEN = "{";
    private static final String CLOSE = "}";

    private SharedresourceItem nextSharedresourceItem(Iterator<String> lines) throws Exception {
	boolean open = false;
	SharedresourceItem item = null;
	while(lines.hasNext()) {
	    String line = lines.next();
	    if (open) {
		if (item == null) item = Factories.sc.windows.createSharedresourceItem();
		if (line.startsWith("Name: ")) {
		    EntityItemStringType netname =  Factories.sc.core.createEntityItemStringType();
		    netname.setValue(line.substring(6));
		    item.setNetname(netname);
		} else if (line.startsWith("Type: ")) {
		    EntityItemSharedResourceTypeType sharedType = Factories.sc.windows.createEntityItemSharedResourceTypeType();
		    try {
			sharedType.setValue(SType.fromCode(parseLong(line.substring(6))).toString());
		    } catch (NumberFormatException e) {
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			sharedType.setStatus(StatusEnumeration.ERROR);
		    }
		    item.setSharedType(sharedType);
		} else if (line.startsWith("Path: ")) {
		    EntityItemStringType localPath =  Factories.sc.core.createEntityItemStringType();
		    localPath.setValue(line.substring(6));
		    item.setLocalPath(localPath);
		} else if (line.startsWith("Max Uses: ")) {
		    EntityItemIntType maxUses =  Factories.sc.core.createEntityItemIntType();
		    maxUses.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    maxUses.setValue(line.substring(10));
		    item.setMaxUses(maxUses);
		} else if (line.startsWith("Current Uses: ")) {
		    EntityItemIntType currentUses =  Factories.sc.core.createEntityItemIntType();
		    currentUses.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    currentUses.setValue(line.substring(14));
		    item.setCurrentUses(currentUses);
		} else if (line.startsWith("Permissions: ")) {
		    try {
			int permissions = parseInt(line.substring(13));

			EntityItemBoolType accessReadPermission = Factories.sc.core.createEntityItemBoolType();
			accessReadPermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessReadPermission.setValue((permissions & ACCESS_READ) == ACCESS_READ ? "1" : "0");
			item.setAccessReadPermission(accessReadPermission);

			EntityItemBoolType accessWritePermission = Factories.sc.core.createEntityItemBoolType();
			accessWritePermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessWritePermission.setValue((permissions & ACCESS_WRITE) == ACCESS_WRITE ? "1" : "0");
			item.setAccessWritePermission(accessWritePermission);

			EntityItemBoolType accessCreatePermission = Factories.sc.core.createEntityItemBoolType();
			accessCreatePermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessCreatePermission.setValue((permissions & ACCESS_CREATE) == ACCESS_CREATE ? "1" : "0");
			item.setAccessCreatePermission(accessCreatePermission);

			EntityItemBoolType accessExecPermission = Factories.sc.core.createEntityItemBoolType();
			accessExecPermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessExecPermission.setValue((permissions & ACCESS_EXEC) == ACCESS_EXEC ? "1" : "0");
			item.setAccessExecPermission(accessExecPermission);

			EntityItemBoolType accessDeletePermission = Factories.sc.core.createEntityItemBoolType();
			accessDeletePermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessDeletePermission.setValue((permissions & ACCESS_DELETE) == ACCESS_DELETE ? "1" : "0");
			item.setAccessDeletePermission(accessDeletePermission);

			EntityItemBoolType accessAtribPermission = Factories.sc.core.createEntityItemBoolType();
			accessAtribPermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessAtribPermission.setValue((permissions & ACCESS_ATRIB) == ACCESS_ATRIB ? "1" : "0");
			item.setAccessAtribPermission(accessAtribPermission);

			EntityItemBoolType accessPermPermission = Factories.sc.core.createEntityItemBoolType();
			accessPermPermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessPermPermission.setValue((permissions & ACCESS_PERM) == ACCESS_PERM ? "1" : "0");
			item.setAccessPermPermission(accessPermPermission);

			EntityItemBoolType accessAllPermission = Factories.sc.core.createEntityItemBoolType();
			accessAllPermission.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			accessAllPermission.setValue((permissions & ACCESS_ALL) == ACCESS_ALL ? "1" : "0");
			item.setAccessAllPermission(accessAllPermission);
		    } catch (NumberFormatException e) {
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		} else if (line.equals(CLOSE)) {
		    if (item == null) {
			open = false;
		    } else {
			break;
		    }
		}
	    } else if (line.equals(OPEN)) {
		open = true;
	    }
	}
	return item;
    }

    private int parseInt(String s) throws NumberFormatException {
	s = s.trim();
	if (s.startsWith("0x")) {
	    return Integer.parseInt(s.substring(2), 16);
	} else {
	    return Integer.parseInt(s);
	}
    }

    private long parseLong(String s) throws NumberFormatException {
	s = s.trim();
	if (s.startsWith("0x")) {
	    return Long.parseLong(s.substring(2), 16);
	} else {
	    return Long.parseLong(s);
	}
    }

    enum SType {
	STYPE_DISKTREE(0x00000000L),
	STYPE_DISKTREE_SPECIAL(0x80000000L),
	STYPE_DISKTREE_TEMPORARY(0x40000000L),
	STYPE_DISKTREE_SPECIAL_TEMPORARY(0xC0000000L),
	STYPE_PRINTQ(0x00000001L),
	STYPE_PRINTQ_SPECIAL(0x80000001L),
	STYPE_PRINTQ_TEMPORARY(0x40000001L),
	STYPE_PRINTQ_SPECIAL_TEMPORARY(0x80000001L),
	STYPE_DEVICE(0x00000002L),
	STYPE_DEVICE_SPECIAL(0x80000002L),
	STYPE_DEVICE_TEMPORARY(0x40000002L),
	STYPE_DEVICE_SPECIAL_TEMPORARY(0xC0000002L),
	STYPE_IPC(0x00000003L),
	STYPE_IPC_SPECIAL(0x80000003L),
	STYPE_IPC_TEMPORARY(0x40000003L),
	STYPE_IPC_SPECIAL_TEMPORARY(0xC0000003L);

	private long code;

	private SType(long code) {
	    this.code = code;
	}

	static SType fromCode(long code) throws IllegalArgumentException {
	    for (SType st : values()) {
		if (st.code == code) {
		    return st;
		}
	    }
	    throw new IllegalArgumentException("0x" + Long.toString(code, 16));
	}
    }
}
