// Copyright (C) 2012 jOVAL.org.  All rights reserved.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.Fileauditedpermissions53Object;
import scap.oval.definitions.windows.FileauditedpermissionsObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemAuditType;
import scap.oval.systemcharacteristics.windows.FileauditedpermissionsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Fileauditedpermissions and Fileauditedpermissions53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileauditedpermissionsAdapter extends BaseFileAdapter<FileauditedpermissionsItem> {
    public static final int SUCCESSFUL_ACCESS_ACE_FLAG	= 64;
    public static final int FAILED_ACCESS_ACE_FLAG	= 128;

    private IWindowsSession ws;
    private IDirectory directory;
    private Map<String, Map<String, List<AuditRule>>> rules;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    baseInit(session);
	    this.ws = (IWindowsSession)session;
	    classes.add(Fileauditedpermissions53Object.class);
	    classes.add(FileauditedpermissionsObject.class);
	} else {
	    notapplicable.add(Fileauditedpermissions53Object.class);
	    notapplicable.add(FileauditedpermissionsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileauditedpermissionsItem.class;
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Fileauditedpermissions.psm1"));
    }

    protected Collection<FileauditedpermissionsItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	initialize();
	directory = ws.getDirectory();

	String pSid = null, pName = null;
	boolean ig = true;  // INCLUDE GROUPS
	boolean rg = false; // RESOLVE GROUPS
	OperationEnumeration op = OperationEnumeration.EQUALS;
	IWindowsSession.View view = ws.getNativeView();
	if (obj instanceof Fileauditedpermissions53Object) {
	    Fileauditedpermissions53Object fObj = (Fileauditedpermissions53Object)obj;
	    op = fObj.getTrusteeSid().getOperation();
	    pSid = (String)fObj.getTrusteeSid().getValue();
	    if (fObj.isSetBehaviors()) {
		view = getView(fObj.getBehaviors());
		ig = fObj.getBehaviors().getIncludeGroup();
		rg = fObj.getBehaviors().getResolveGroup();
	    }
	} else if (obj instanceof FileauditedpermissionsObject) {
	    FileauditedpermissionsObject fObj = (FileauditedpermissionsObject)obj;
	    op = fObj.getTrusteeName().getOperation();
	    pName = (String)fObj.getTrusteeName().getValue();
	    if (fObj.isSetBehaviors()) {
		view = getView(fObj.getBehaviors());
		ig = fObj.getBehaviors().getIncludeGroup();
		rg = fObj.getBehaviors().getResolveGroup();
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	Collection<FileauditedpermissionsItem> items = new ArrayList<FileauditedpermissionsItem>();
	for (IFile f : files) {
	    try {
		FileauditedpermissionsItem baseItem = (FileauditedpermissionsItem)getBaseItem(obj, f);
		if (baseItem != null) {
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
			for (Map.Entry<String, List<AuditRule>> entry : getAuditRules(f, view).entrySet()) {
			    IPrincipal principal = null;
			    try {
				if (pSid == null) {
				    IPrincipal temp = directory.queryPrincipalBySid(entry.getKey());
				    if (temp.isBuiltin()) {
					if (p.matcher(temp.getName()).find()) {
					    principal = temp;
					}
				    } else if (p.matcher(temp.getNetbiosName()).find()) {
					principal = temp;
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
			    principals = directory.getAllPrincipals(directory.queryPrincipal(pName), ig, rg);
			} else {
			    principals = directory.getAllPrincipals(directory.queryPrincipalBySid(pSid), ig, rg);
			}
			Map<String, List<AuditRule>> auditRules = getAuditRules(f, view);
			for (IPrincipal principal : principals) {
			    switch(op) {
			      case EQUALS:
			      case CASE_INSENSITIVE_EQUALS:
				items.add(makeItem(baseItem, principal, auditRules.get(principal.getSid())));
				break;
			      case NOT_EQUAL:
				items.add(makeItem(baseItem, principal, auditRules.get(principal.getSid())));
				break;
			    }
			}
			break;

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
		}
	    } catch (PatternSyntaxException e) {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    } catch (NoSuchElementException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
		rc.addMessage(msg);
	    } catch (WmiException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
		rc.addMessage(msg);
	    } catch (PowershellException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_FILESACL, obj.getId(), e.getMessage()));
		rc.addMessage(msg);
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void initialize() {
	if (rules == null) {
	    rules = new HashMap<String, Map<String, List<AuditRule>>>();
	} else {
	    return; // previously initialized
	}
    }

    /**
     * Retrieve the access entries for the file.
     */
    private Map<String, List<AuditRule>> getAuditRules(IFile f, IWindowsSession.View view) throws Exception {
	String path = f.getPath();
	if (rules.containsKey(path)) {
	    return rules.get(path);
	} else {
	    Map<String, List<AuditRule>> fileAuditRules = new HashMap<String, List<AuditRule>>();
	    rules.put(path, fileAuditRules);

	    String data = getRunspace(view).invoke("Get-FileAuditedPermissions -Path \"" + path + "\"");
	    if (data != null) {
		for (String entry : data.split("\r\n")) {
		    int ptr1 = entry.indexOf(":");
		    int ptr2 = entry.indexOf(",");
		    String sid = entry.substring(0,ptr1).trim();
		    int mask = Integer.valueOf(entry.substring(ptr1+1,ptr2).trim());
		    int flags = Integer.valueOf(entry.substring(ptr2+1).trim());

		    if (!fileAuditRules.containsKey(sid)) {
			fileAuditRules.put(sid, new ArrayList<AuditRule>());
		    }

		    fileAuditRules.get(sid).add(new AuditRule(sid, mask, flags));
		}
	    }
	    return fileAuditRules;
	}
    }

    private String toAuditValue(AuditRule rule) {
	boolean success = SUCCESSFUL_ACCESS_ACE_FLAG == (SUCCESSFUL_ACCESS_ACE_FLAG | rule.getFlags());
	boolean fail = FAILED_ACCESS_ACE_FLAG == (FAILED_ACCESS_ACE_FLAG | rule.getFlags());

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
    private FileauditedpermissionsItem makeItem(FileauditedpermissionsItem base, IPrincipal p, List<AuditRule> rules) {
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

	if (rules != null) {
	    for (AuditRule rule : rules) {
		if (IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY & rule.getAccessMask())) {
		    EntityItemAuditType accessSystemSecurity = Factories.sc.windows.createEntityItemAuditType();
		    accessSystemSecurity.setValue(toAuditValue(rule));
		    item.setAccessSystemSecurity(accessSystemSecurity);
		}
		if (IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA & rule.getAccessMask())) {
		    EntityItemAuditType fileAppendData = Factories.sc.windows.createEntityItemAuditType();
		    fileAppendData.setValue(toAuditValue(rule));
		    item.setFileAppendData(fileAppendData);
		}
		if (IACE.FILE_DELETE == (IACE.FILE_DELETE & rule.getAccessMask())) {
		    EntityItemAuditType fileDeleteChild = Factories.sc.windows.createEntityItemAuditType();
		    fileDeleteChild.setValue(toAuditValue(rule));
		    item.setFileDeleteChild(fileDeleteChild);
		}
		if (IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE & rule.getAccessMask())) {
		    EntityItemAuditType fileExecute = Factories.sc.windows.createEntityItemAuditType();
		    fileExecute.setValue(toAuditValue(rule));
		    item.setFileExecute(fileExecute);
		}
		if (IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES & rule.getAccessMask())) {
		    EntityItemAuditType fileReadAttributes = Factories.sc.windows.createEntityItemAuditType();
		    fileReadAttributes.setValue(toAuditValue(rule));
		    item.setFileReadAttributes(fileReadAttributes);
		}
		if (IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA & rule.getAccessMask())) {
		    EntityItemAuditType fileReadData = Factories.sc.windows.createEntityItemAuditType();
		    fileReadData.setValue(toAuditValue(rule));
		    item.setFileReadData(fileReadData);
		}
		if (IACE.FILE_READ_EA == (IACE.FILE_READ_EA & rule.getAccessMask())) {
		    EntityItemAuditType fileReadEa = Factories.sc.windows.createEntityItemAuditType();
		    fileReadEa.setValue(toAuditValue(rule));
		    item.setFileReadEa(fileReadEa);
		}
		if (IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES & rule.getAccessMask())) {
		    EntityItemAuditType fileWriteAttributes = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteAttributes.setValue(toAuditValue(rule));
		    item.setFileWriteAttributes(fileWriteAttributes);
		}
		if (IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA & rule.getAccessMask())) {
		    EntityItemAuditType fileWriteData = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteData.setValue(toAuditValue(rule));
		    item.setFileWriteData(fileWriteData);
		}
		if (IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA & rule.getAccessMask())) {
		    EntityItemAuditType fileWriteEa = Factories.sc.windows.createEntityItemAuditType();
		    fileWriteEa.setValue(toAuditValue(rule));
		    item.setFileWriteEa(fileWriteEa);
		}
		if (IACE.FILE_GENERIC_ALL == (IACE.FILE_GENERIC_ALL & rule.getAccessMask())) {
		    EntityItemAuditType genericAll = Factories.sc.windows.createEntityItemAuditType();
		    genericAll.setValue(toAuditValue(rule));
		    item.setGenericAll(genericAll);
		}
		if (IACE.FILE_GENERIC_EXECUTE == (IACE.FILE_GENERIC_EXECUTE & rule.getAccessMask())) {
		    EntityItemAuditType genericExecute = Factories.sc.windows.createEntityItemAuditType();
		    genericExecute.setValue(toAuditValue(rule));
		    item.setGenericExecute(genericExecute);
		}
		if (IACE.FILE_GENERIC_READ == (IACE.FILE_GENERIC_READ & rule.getAccessMask())) {
		    EntityItemAuditType genericRead = Factories.sc.windows.createEntityItemAuditType();
		    genericRead.setValue(toAuditValue(rule));
		    item.setGenericRead(genericRead);
		}
		if (IACE.FILE_GENERIC_WRITE == (IACE.FILE_GENERIC_WRITE & rule.getAccessMask())) {
		    EntityItemAuditType genericWrite = Factories.sc.windows.createEntityItemAuditType();
		    genericWrite.setValue(toAuditValue(rule));
		    item.setGenericWrite(genericWrite);
		}
		if (IACE.DELETE == (IACE.DELETE & rule.getAccessMask())) {
		    EntityItemAuditType standardDelete = Factories.sc.windows.createEntityItemAuditType();
		    standardDelete.setValue(toAuditValue(rule));
		    item.setStandardDelete(standardDelete);
		}
		if (IACE.READ_CONTROL == (IACE.READ_CONTROL & rule.getAccessMask())) {
		    EntityItemAuditType standardReadControl = Factories.sc.windows.createEntityItemAuditType();
		    standardReadControl.setValue(toAuditValue(rule));
		    item.setStandardReadControl(standardReadControl);
		}
		if (IACE.SYNCHRONIZE == (IACE.SYNCHRONIZE & rule.getAccessMask())) {
		    EntityItemAuditType standardSynchronize = Factories.sc.windows.createEntityItemAuditType();
		    standardSynchronize.setValue(toAuditValue(rule));
		    item.setStandardSynchronize(standardSynchronize);
		}
		if (IACE.WRITE_DAC == (IACE.WRITE_DAC & rule.getAccessMask())) {
		    EntityItemAuditType standardWriteDac = Factories.sc.windows.createEntityItemAuditType();
		    standardWriteDac.setValue(toAuditValue(rule));
		    item.setStandardWriteDac(standardWriteDac);
		}
		if (IACE.WRITE_OWNER == (IACE.WRITE_OWNER & rule.getAccessMask())) {
		    EntityItemAuditType standardWriteOwner = Factories.sc.windows.createEntityItemAuditType();
		    standardWriteOwner.setValue(toAuditValue(rule));
		    item.setStandardWriteOwner(standardWriteOwner);
		}
	    }
	}

	EntityItemStringType trusteeName = Factories.sc.core.createEntityItemStringType();
	if (p.isBuiltin()) {
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

    class AuditRule implements IACE {
	private int mask, flags;
	private String sid;

	AuditRule(String sid, int mask, int flags) {
	    this.sid = sid;
	    this.mask = mask;
	    this.flags = flags;
	}

	public int getFlags() {
	    return flags;
	}

	// Implement IACE

	public int getAccessMask() {
	    return mask;
	}

	public String getSid() {
	    return sid;
	}
    }
}
