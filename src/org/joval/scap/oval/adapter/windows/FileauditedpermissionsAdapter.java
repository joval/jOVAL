// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.Fileauditedpermissions53Object;
import oval.schemas.definitions.windows.FileauditedpermissionsObject;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.EntityItemAuditType;
import oval.schemas.systemcharacteristics.windows.FileauditedpermissionsItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.os.windows.wmi.WmiException;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Collects items for Fileauditedpermissions and Fileauditedpermissions53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileauditedpermissionsAdapter extends BaseFileAdapter<FileauditedpermissionsItem> {
    private IWindowsSession ws;
    private IDirectory directory;
    private IRunspace runspace;
    private Map<String, Map<String, List<IACE>>> acls;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((ISession)session);
	    this.ws = (IWindowsSession)session;
	    classes.add(Fileauditedpermissions53Object.class);
	    classes.add(FileauditedpermissionsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileauditedpermissionsItem.class;
    }

    protected Collection<FileauditedpermissionsItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	initialize();
	if (runspace == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL), FlagEnumeration.NOT_COLLECTED);
	}

	//
	// Grab a fresh directory in case there's been a reconnect since initialization.
	//
	directory = ws.getDirectory();

	Collection<FileauditedpermissionsItem> items = new Vector<FileauditedpermissionsItem>();

	FileauditedpermissionsItem baseItem = null;
	if (base instanceof FileauditedpermissionsItem) {
	    baseItem = (FileauditedpermissionsItem)base;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	String pSid = null, pName = null;
	boolean includeGroups = true;
	boolean resolveGroups = false;
	OperationEnumeration op = OperationEnumeration.EQUALS;
	if (obj instanceof Fileauditedpermissions53Object) {
	    Fileauditedpermissions53Object fObj = (Fileauditedpermissions53Object)obj;
	    op = fObj.getTrusteeSid().getOperation();
	    pSid = (String)fObj.getTrusteeSid().getValue();
	    if (fObj.isSetBehaviors()) {
		includeGroups = fObj.getBehaviors().getIncludeGroup();
		resolveGroups = fObj.getBehaviors().getResolveGroup();
	    }
	} else if (obj instanceof FileauditedpermissionsObject) {
	    FileauditedpermissionsObject fObj = (FileauditedpermissionsObject)obj;
	    op = fObj.getTrusteeName().getOperation();
	    pName = (String)fObj.getTrusteeName().getValue();
	    if (fObj.isSetBehaviors()) {
		includeGroups = fObj.getBehaviors().getIncludeGroup();
		resolveGroups = fObj.getBehaviors().getResolveGroup();
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	try {
	    switch(op) {
	      case PATTERN_MATCH:
		Pattern p = null;
		if (pSid == null) {
		    p = Pattern.compile(pName);
		} else {
		    p = Pattern.compile(pSid);
		}
		//
		// Note: per the specification, the scope is limited to the trustees referenced by the security
		// descriptor, as opposed to the full scope of all known trustees.
		//
		for (Map.Entry<String, List<IACE>> entry : getAccessEntries(f.getPath()).entrySet()) {
		    IPrincipal principal = null;
		    try {
			if (pSid == null) {
			    IPrincipal temp = directory.queryPrincipalBySid(entry.getKey());
			    if (directory.isBuiltinUser(temp.getNetbiosName()) ||
				directory.isBuiltinGroup(temp.getNetbiosName())) {
				if (p.matcher(temp.getName()).find()) {
				    principal = temp;
				}
			    } else {
				if (p.matcher(temp.getNetbiosName()).find()) {
				    principal = temp;
				}
			    }
			} else {
			    if (p.matcher(entry.getKey()).find()) {
				principal = directory.queryPrincipalBySid(entry.getKey());
			    }
			}
			if (principal != null) {
			    items.add(makeItem(baseItem, principal, entry.getValue()));
			}
		    } catch (NoSuchElementException e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.WARNING);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
			rc.addMessage(msg);
		    }
		}
		break;

	      case CASE_INSENSITIVE_EQUALS:
	      case EQUALS:
	      case NOT_EQUAL:
		Collection<IPrincipal> principals = null;
		if (pSid == null) {
		    principals = directory.getAllPrincipals(directory.queryPrincipal(pName), includeGroups, resolveGroups);
		} else {
		    principals = directory.getAllPrincipals(directory.queryPrincipalBySid(pSid), includeGroups, resolveGroups);
		}
		Map<String, List<IACE>> aces = getAccessEntries(f.getPath());
		for (IPrincipal principal : principals) {
		    switch(op) {
		      case EQUALS:
		      case CASE_INSENSITIVE_EQUALS:
			items.add(makeItem(baseItem, principal, aces.get(principal.getSid())));
			break;
		      case NOT_EQUAL:
			items.add(makeItem(baseItem, principal, aces.get(principal.getSid())));
			break;
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
	    rc.addMessage(msg);
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	} catch (PowershellException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_FILESACL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void initialize() {
	if (acls == null) {
	    acls = new HashMap<String, Map<String, List<IACE>>>();
	} else {
	    return; // previously initialized
	}

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Cmdlet utilities
	// Powershell module code.
	//
	for (IRunspace rs : ws.getRunspacePool().enumerate()) {
	    runspace = rs;
	    break;
	}
	try {
	    if (runspace == null) {
		runspace = ws.getRunspacePool().spawn();
	    }
	    if (runspace != null) {
		runspace.loadModule(getClass().getResourceAsStream("Fileauditedpermissions.psm1"));
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Retrieve the access entries for the file.
     */
    private Map<String, List<IACE>> getAccessEntries(String path)
		throws IOException, NoSuchElementException, PowershellException {

	if (acls.containsKey(path)) {
	    return acls.get(path);
	} else {
	    Map<String, List<IACE>> aces = new HashMap<String, List<IACE>>();
	    acls.put(path, aces);

	    String pathArg = path.replace("\\", "\\\\");
	    if (path.indexOf(" ") != -1) {
		if (!path.startsWith("\"") && !path.endsWith("\"")) {
		    pathArg = new StringBuffer("\"").append(pathArg).append("\"").toString();
		}
	    }
	    String data = runspace.invoke("Get-FileAuditedPermissions " + pathArg);
	    if (data != null) {
		for (String entry : data.split("\r\n")) {
		    int ptr1 = entry.indexOf(":");
		    int ptr2 = entry.indexOf(",");
		    String sid = entry.substring(0,ptr1).trim();
		    int mask = Integer.valueOf(entry.substring(ptr1+1,ptr2).trim());
		    int flags = Integer.valueOf(entry.substring(ptr2+1).trim());

		    if (!aces.containsKey(sid)) {
			aces.put(sid, new ArrayList<IACE>());
		    }

		    aces.get(sid).add(new ACE(sid, mask, flags));
		}
	    }
	    return aces;
	}
    }

    private String toAuditValue(IACE ace) {
	boolean success = IACE.SUCCESSFUL_ACCESS_ACE_FLAG == (IACE.SUCCESSFUL_ACCESS_ACE_FLAG | ace.getFlags());
	boolean fail = IACE.FAILED_ACCESS_ACE_FLAG == (IACE.FAILED_ACCESS_ACE_FLAG | ace.getFlags());

	if (success && fail) {
	    return "AUDIT_SUCCESS_FAILURE";
	} else if (success) {
	    return "AUDIT_SUCCESS";
	} else if (fail) {
	    return "AUDIT_FAILURE";
	} else {
	    return "AUDIT_NONE";
	}
    }

    /**
     * Create a new wrapped FileauditedpermissionsItem based on the base FileauditedpermissionsItem, IPrincipal and mask.
     */
    private FileauditedpermissionsItem makeItem(FileauditedpermissionsItem base, IPrincipal p, List<IACE> aces) {
	FileauditedpermissionsItem item = Factories.sc.windows.createFileauditedpermissionsItem();
	item.setPath(base.getPath());
	item.setFilename(base.getFilename());
	item.setFilepath(base.getFilepath());
	item.setWindowsView(base.getWindowsView());

	//
	// By default, set all the permissions to none.
	//
	EntityItemAuditType audit_none = Factories.sc.windows.createEntityItemAuditType();
	audit_none.setValue("AUDIT_NONE");
	item.setAccessSystemSecurity(audit_none);
	item.setFileAppendData(audit_none);
	item.setFileDeleteChild(audit_none);
	item.setFileExecute(audit_none);
	item.setFileReadAttributes(audit_none);
	item.setFileReadData(audit_none);
	item.setFileReadEa(audit_none);
	item.setFileWriteAttributes(audit_none);
	item.setFileWriteData(audit_none);
	item.setFileWriteEa(audit_none);
	item.setGenericAll(audit_none);
	item.setGenericExecute(audit_none);
	item.setGenericRead(audit_none);
	item.setGenericWrite(audit_none);
	item.setStandardDelete(audit_none);
	item.setStandardReadControl(audit_none);
	item.setStandardSynchronize(audit_none);
	item.setStandardWriteDac(audit_none);
	item.setStandardWriteOwner(audit_none);

	if (aces != null) {
	    for (IACE ace : aces) {
		if (IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY & ace.getAccessMask())) {
		    EntityItemAuditType accessSystemSecurity = Factories.sc.windows.createEntityItemAuditType();
		    accessSystemSecurity.setValue(toAuditValue(ace));
		    item.setAccessSystemSecurity(accessSystemSecurity);
		}
		if (IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA & ace.getAccessMask())) {
		    EntityItemAuditType fileAppendData = Factories.sc.windows.createEntityItemAuditType();
		    fileAppendData.setValue(toAuditValue(ace));
		    item.setFileAppendData(fileAppendData);
		}
		if (IACE.FILE_DELETE == (IACE.FILE_DELETE & ace.getAccessMask())) {
		    EntityItemAuditType fileDeleteChild = Factories.sc.windows.createEntityItemAuditType();
		    fileDeleteChild.setValue(toAuditValue(ace));
		    item.setFileDeleteChild(fileDeleteChild);
		}
		if (IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE & ace.getAccessMask())) {
		    EntityItemAuditType fileExecute = Factories.sc.windows.createEntityItemAuditType();
		    fileExecute.setValue(toAuditValue(ace));
		    item.setFileExecute(fileExecute);
		}
		if (IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES & ace.getAccessMask())) {
		    EntityItemAuditType fileReadAttributes = Factories.sc.windows.createEntityItemAuditType();
		    fileReadAttributes.setValue(toAuditValue(ace));
		    item.setFileReadAttributes(fileReadAttributes);
		}
		if (IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA & ace.getAccessMask())) {
		    EntityItemAuditType fileReadData = Factories.sc.windows.createEntityItemAuditType();
		    fileReadData.setValue(toAuditValue(ace));
		    item.setFileReadData(fileReadData);
		}
		if (IACE.FILE_READ_EA == (IACE.FILE_READ_EA & ace.getAccessMask())) {
		    EntityItemAuditType fileReadEa = Factories.sc.windows.createEntityItemAuditType();
		    fileReadEa.setValue(toAuditValue(ace));
		    item.setFileReadEa(fileReadEa);
		}
		if (IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES & ace.getAccessMask())) {
		    EntityItemAuditType fileWriteAttributes = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteAttributes.setValue(toAuditValue(ace));
		    item.setFileWriteAttributes(fileWriteAttributes);
		}
		if (IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA & ace.getAccessMask())) {
		    EntityItemAuditType fileWriteData = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteData.setValue(toAuditValue(ace));
		    item.setFileWriteData(fileWriteData);
		}
		if (IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA & ace.getAccessMask())) {
		    EntityItemAuditType fileWriteEa = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteEa.setValue(toAuditValue(ace));
		    item.setFileWriteEa(fileWriteEa);
		}
		if (IACE.GENERIC_ALL == (IACE.GENERIC_ALL & ace.getAccessMask())) {
		    EntityItemAuditType genericAll = Factories.sc.windows.createEntityItemAuditType();
		    genericAll.setValue(toAuditValue(ace));
		    item.setGenericAll(genericAll);
		}
		if (IACE.GENERIC_EXECUTE == (IACE.GENERIC_EXECUTE & ace.getAccessMask())) {
		    EntityItemAuditType genericExecute = Factories.sc.windows.createEntityItemAuditType();
		    genericExecute.setValue(toAuditValue(ace));
		    item.setGenericExecute(genericExecute);
		}
		if (IACE.GENERIC_READ == (IACE.GENERIC_READ & ace.getAccessMask())) {
		    EntityItemAuditType genericRead = Factories.sc.windows.createEntityItemAuditType();
		    genericRead.setValue(toAuditValue(ace));
		    item.setGenericRead(genericRead);
		}
		if (IACE.GENERIC_WRITE == (IACE.GENERIC_WRITE & ace.getAccessMask())) {
		    EntityItemAuditType genericWrite = Factories.sc.windows.createEntityItemAuditType();
		    genericWrite.setValue(toAuditValue(ace));
		    item.setGenericWrite(genericWrite);
		}
		if (IACE.STANDARD_DELETE == (IACE.STANDARD_DELETE & ace.getAccessMask())) {
		    EntityItemAuditType standardDelete = Factories.sc.windows.createEntityItemAuditType();
		    standardDelete.setValue(toAuditValue(ace));
		    item.setStandardDelete(standardDelete);
		}
		if (IACE.STANDARD_READ_CONTROL == (IACE.STANDARD_READ_CONTROL & ace.getAccessMask())) {
		    EntityItemAuditType standardReadControl = Factories.sc.windows.createEntityItemAuditType();
		    standardReadControl.setValue(toAuditValue(ace));
		    item.setStandardReadControl(standardReadControl);
		}
		if (IACE.STANDARD_SYNCHRONIZE == (IACE.STANDARD_SYNCHRONIZE & ace.getAccessMask())) {
		    EntityItemAuditType standardSynchronize = Factories.sc.windows.createEntityItemAuditType();
		    standardSynchronize.setValue(toAuditValue(ace));
		    item.setStandardSynchronize(standardSynchronize);
		}
		if (IACE.STANDARD_WRITE_DAC == (IACE.STANDARD_WRITE_DAC & ace.getAccessMask())) {
		    EntityItemAuditType standardWriteDac = Factories.sc.windows.createEntityItemAuditType();
		    standardWriteDac.setValue(toAuditValue(ace));
		    item.setStandardWriteDac(standardWriteDac);
		}
		if (IACE.STANDARD_WRITE_OWNER == (IACE.STANDARD_WRITE_OWNER & ace.getAccessMask())) {
		    EntityItemAuditType standardWriteOwner = Factories.sc.windows.createEntityItemAuditType();
		    standardWriteOwner.setValue(toAuditValue(ace));
		    item.setStandardWriteOwner(standardWriteOwner);
		}
	    }
	}

	EntityItemStringType trusteeName = Factories.sc.core.createEntityItemStringType();
	if (directory.isBuiltinUser(p.getNetbiosName()) || directory.isBuiltinGroup(p.getNetbiosName())) {
	    trusteeName.setValue(p.getName());
	} else {
	    trusteeName.setValue(p.getNetbiosName());
	}
	item.setTrusteeName(trusteeName);

	EntityItemStringType trusteeSid = Factories.sc.core.createEntityItemStringType();
	trusteeSid.setValue(p.getSid());
	item.setTrusteeSid(trusteeSid);

	return item;
    }

    class ACE implements IACE {
	private String sid;
	private int mask, flags;

	ACE(String sid, int mask, int flags) {
	    this.sid = sid;
	    this.mask = mask;
	    this.flags = flags;
	}

	public int getFlags() {
	    return flags;
	}

	public int getAccessMask() {
	    return mask;
	}

	public String getSid() {
	    return sid;
	}
    }
}
