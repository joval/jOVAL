// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;
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
import oval.schemas.definitions.windows.Fileeffectiverights53Object;
import oval.schemas.definitions.windows.FileeffectiverightsObject;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.FileeffectiverightsItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.io.IWindowsFileInfo;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Collects items for Fileeffectiverights and Fileeffectiverights53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileeffectiverightsAdapter extends BaseFileAdapter<FileeffectiverightsItem> {
    private IWindowsSession ws;
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((ISession)session);
	    this.ws = (IWindowsSession)session;
	    classes.add(Fileeffectiverights53Object.class);
	    classes.add(FileeffectiverightsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileeffectiverightsItem.class;
    }

    protected Collection<FileeffectiverightsItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {
	//
	// Grab a fresh directory in case there's been a reconnect since initialization.
	//
	directory = ws.getDirectory();

	Collection<FileeffectiverightsItem> items = new Vector<FileeffectiverightsItem>();

	FileeffectiverightsItem baseItem = null;
	if (base instanceof FileeffectiverightsItem) {
	    baseItem = (FileeffectiverightsItem)base;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	String pSid = null, pName = null;
	boolean includeGroups = true;
	boolean resolveGroups = false;
	OperationEnumeration op = OperationEnumeration.EQUALS;
	if (obj instanceof Fileeffectiverights53Object) {
	    Fileeffectiverights53Object fObj = (Fileeffectiverights53Object)obj;
	    op = fObj.getTrusteeSid().getOperation();
	    pSid = (String)fObj.getTrusteeSid().getValue();
	    if (fObj.isSetBehaviors()) {
		includeGroups = fObj.getBehaviors().getIncludeGroup();
		resolveGroups = fObj.getBehaviors().getResolveGroup();
	    }
	} else if (obj instanceof FileeffectiverightsObject) {
	    FileeffectiverightsObject fObj = (FileeffectiverightsObject)obj;
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

	IFileEx info = f.getExtended();
	IWindowsFileInfo wfi = null;
	if (info instanceof IWindowsFileInfo) {
	    wfi = (IWindowsFileInfo)info;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_TYPE, f.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	IACE[] aces = wfi.getSecurity();

	switch(op) {
	  case PATTERN_MATCH:
	    try {
		Pattern p = null;
		if (pSid == null) {
		    p = Pattern.compile(pName);
		} else {
		    p = Pattern.compile(pSid);
		}
		for (int i=0; i < aces.length; i++) {
		    IACE ace = aces[i];
		    IPrincipal principal = null;
		    try {
			if (pSid == null) {
			    IPrincipal temp = directory.queryPrincipalBySid(ace.getSid());
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
			    if (p.matcher(ace.getSid()).find()) {
				principal = directory.queryPrincipalBySid(ace.getSid());
			    }
			}
			if (principal != null) {
			    items.add(makeItem(baseItem, principal, ace));
			}
		    } catch (NoSuchElementException e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.WARNING);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINDIR_NOPRINCIPAL, e.getMessage()));
			rc.addMessage(msg);
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (WmiException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
		rc.addMessage(msg);
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	  case EQUALS:
	  case NOT_EQUAL:
	    try {
		Collection<IPrincipal> principals = null;
		if (pSid == null) {
		    principals = directory.getAllPrincipals(directory.queryPrincipal(pName), includeGroups, resolveGroups);
		} else {
		    principals = directory.getAllPrincipals(directory.queryPrincipalBySid(pSid), includeGroups, resolveGroups);
		}
		for (IPrincipal principal : principals) {
		    for (int i=0; i < aces.length; i++) {
			switch(op) {
			  case EQUALS:
			  case CASE_INSENSITIVE_EQUALS:
			    if (directory.isApplicable(principal, aces[i])) {
				items.add(makeItem(baseItem, principal, aces[i]));
			    }
			    break;
			  case NOT_EQUAL:
			    if (!directory.isApplicable(principal, aces[i])) {
				items.add(makeItem(baseItem, principal, aces[i]));
			    }
			    break;
			}
		    }
		}
	    } catch (NoSuchElementException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		if (pSid == null) {
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINDIR_NOPRINCIPAL, pName));
		} else {
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINDIR_NOPRINCIPAL, pSid));
		}
		rc.addMessage(msg);
	    } catch (WmiException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
		rc.addMessage(msg);
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
     * Create a new wrapped FileeffectiverightsItem based on the base FileeffectiverightsItem, IPrincipal and IACE.
     */
    private FileeffectiverightsItem makeItem(FileeffectiverightsItem base, IPrincipal p, IACE ace) throws IOException {
	FileeffectiverightsItem item = Factories.sc.windows.createFileeffectiverightsItem();
	item.setPath(base.getPath());
	item.setFilename(base.getFilename());
	item.setFilepath(base.getFilepath());
	item.setWindowsView(base.getWindowsView());

	int accessMask = ace.getAccessMask();
	boolean test = false;

	test = IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY | accessMask);
	EntityItemBoolType accessSystemSecurity = Factories.sc.core.createEntityItemBoolType();
	accessSystemSecurity.setValue(Boolean.toString(test));
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

	test = IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA | accessMask);
	EntityItemBoolType fileAppendData = Factories.sc.core.createEntityItemBoolType();
	fileAppendData.setValue(Boolean.toString(test));
	fileAppendData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileAppendData(fileAppendData);

	test = IACE.FILE_DELETE == (IACE.FILE_DELETE | accessMask);
	EntityItemBoolType fileDeleteChild = Factories.sc.core.createEntityItemBoolType();
	fileDeleteChild.setValue(Boolean.toString(test));
	fileDeleteChild.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileDeleteChild(fileDeleteChild);

	test = IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE | accessMask);
	EntityItemBoolType fileExecute = Factories.sc.core.createEntityItemBoolType();
	fileExecute.setValue(Boolean.toString(test));
	fileExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileExecute(fileExecute);

	test = IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES | accessMask);
	EntityItemBoolType fileReadAttributes = Factories.sc.core.createEntityItemBoolType();
	fileReadAttributes.setValue(Boolean.toString(test));
	fileReadAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadAttributes(fileReadAttributes);

	test = IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA | accessMask);
	EntityItemBoolType fileReadData = Factories.sc.core.createEntityItemBoolType();
	fileReadData.setValue(Boolean.toString(test));
	fileReadData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadData(fileReadData);

	test = IACE.FILE_READ_EA == (IACE.FILE_READ_EA | accessMask);
	EntityItemBoolType fileReadEa = Factories.sc.core.createEntityItemBoolType();
	fileReadEa.setValue(Boolean.toString(test));
	fileReadEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadEa(fileReadEa);

	test = IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES | accessMask);
	EntityItemBoolType fileWriteAttributes = Factories.sc.core.createEntityItemBoolType();
	fileWriteAttributes.setValue(Boolean.toString(test));
	fileWriteAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteAttributes(fileWriteAttributes);

	test = IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA | accessMask);
	EntityItemBoolType fileWriteData = Factories.sc.core.createEntityItemBoolType();
	fileWriteData.setValue(Boolean.toString(test));
	fileWriteData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteData(fileWriteData);

	test = IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA | accessMask);
	EntityItemBoolType fileWriteEa = Factories.sc.core.createEntityItemBoolType();
	fileWriteEa.setValue(Boolean.toString(test));
	fileWriteEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteEa(fileWriteEa);

	test = IACE.GENERIC_ALL == (IACE.GENERIC_ALL | accessMask);
	EntityItemBoolType genericAll = Factories.sc.core.createEntityItemBoolType();
	genericAll.setValue(Boolean.toString(test));
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericAll(genericAll);

	test = IACE.GENERIC_EXECUTE == (IACE.GENERIC_EXECUTE | accessMask);
	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(Boolean.toString(test));
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.GENERIC_READ == (IACE.GENERIC_READ | accessMask);
	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(Boolean.toString(test));
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.GENERIC_WRITE == (IACE.GENERIC_WRITE | accessMask);
	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(Boolean.toString(test));
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.STANDARD_DELETE == (IACE.STANDARD_DELETE | accessMask);
	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(Boolean.toString(test));
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.STANDARD_READ_CONTROL == (IACE.STANDARD_READ_CONTROL | accessMask);
	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(Boolean.toString(test));
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.STANDARD_SYNCHRONIZE == (IACE.STANDARD_SYNCHRONIZE | accessMask);
	EntityItemBoolType standardSynchronize = Factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setValue(Boolean.toString(test));
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardSynchronize(standardSynchronize);

	test = IACE.STANDARD_WRITE_DAC == (IACE.STANDARD_WRITE_DAC | accessMask);
	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(Boolean.toString(test));
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.STANDARD_WRITE_OWNER == (IACE.STANDARD_WRITE_OWNER | accessMask);
	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(Boolean.toString(test));
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteOwner(standardWriteOwner);

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
}
