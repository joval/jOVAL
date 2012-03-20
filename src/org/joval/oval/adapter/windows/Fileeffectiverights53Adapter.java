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
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.windows.Fileeffectiverights53Object;
import oval.schemas.definitions.windows.FileEffectiveRights53Behaviors;
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
import org.joval.oval.OvalException;
import org.joval.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Collects items for Fileeffectiverights53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Fileeffectiverights53Adapter extends BaseFileAdapter {
    private IWindowsSession ws;
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((ISession)session);
	    this.ws = (IWindowsSession)session;
	    classes.add(Fileeffectiverights53Object.class);
	}
	return classes;
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return JOVALSystem.factories.sc.windows.createFileeffectiverightsItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.windows.createFileeffectiverightsItem();
    }

    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException, OvalException {

	directory = ws.getDirectory();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	if (base instanceof FileeffectiverightsItem) {
	    FileeffectiverightsItem baseItem = (FileeffectiverightsItem)base;

	    IFileEx info = f.getExtended();
	    IWindowsFileInfo wfi = null;
	    if (info instanceof IWindowsFileInfo) {
		wfi = (IWindowsFileInfo)info;
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINFILE_TYPE, f.getClass().getName());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    IACE[] aces = wfi.getSecurity();
	    Fileeffectiverights53Object fObj = (Fileeffectiverights53Object)rc.getObject();
	    String sid = (String)fObj.getTrusteeSid().getValue();

	    OperationEnumeration op = fObj.getTrusteeSid().getOperation();
	    switch(op) {
	      case PATTERN_MATCH:
		try {
		    Pattern p = Pattern.compile(sid);
		    for (int i=0; i < aces.length; i++) {
			IACE ace = aces[i];
			if (p.matcher(ace.getSid()).find()) {
			    IPrincipal principal = directory.queryPrincipalBySid(ace.getSid());
			    items.add(makeItem(baseItem, principal, ace));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		} catch (WmiException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
		    rc.addMessage(msg);
		}
		break;

	      case EQUALS:
	      case NOT_EQUAL:
		try {
		    Collection<IPrincipal> principals = getPrincipals(directory.queryPrincipalBySid(sid), fObj.getBehaviors());
		    for (IPrincipal principal : principals) {
			for (int i=0; i < aces.length; i++) {
			    if (op == OperationEnumeration.EQUALS && directory.isApplicable(principal, aces[i])) {
				items.add(makeItem(baseItem, principal, aces[i]));
			    } else if (op == OperationEnumeration.NOT_EQUAL && !directory.isApplicable(principal, aces[i])) {
				items.add(makeItem(baseItem, principal, aces[i]));
			    }
			}
		    }
		} catch (NoSuchElementException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINDIR_NOPRINCIPAL, sid));
		    rc.addMessage(msg);
		} catch (WmiException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, e.getMessage()));
		    rc.addMessage(msg);
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	return items;
    }

    // Private

    /**
     * Recursively fetch all members of the IPrincipal (if it's a group) according to the specified behaviors.
     */
    private Collection<IPrincipal> getPrincipals(IPrincipal principal, FileEffectiveRights53Behaviors behaviors)
		throws WmiException {

	boolean includeGroups = true;
	boolean resolveGroups = false;
	if (behaviors != null) {
	    includeGroups = behaviors.isIncludeGroup();
	    resolveGroups = behaviors.isResolveGroup();
	}
	return directory.getAllPrincipals(principal, includeGroups, resolveGroups);
    }

    /**
     * Create a new wrapped FileeffectiverightsItem based on the base FileeffectiverightsItem, IPrincipal and IACE.
     */
    private JAXBElement<FileeffectiverightsItem> makeItem(FileeffectiverightsItem base, IPrincipal p, IACE ace)
		throws IOException {

	FileeffectiverightsItem item = JOVALSystem.factories.sc.windows.createFileeffectiverightsItem();
	item.setPath(base.getPath());
	item.setFilename(base.getFilename());
	item.setFilepath(base.getFilepath());
	item.setWindowsView(base.getWindowsView());

	int accessMask = ace.getAccessMask();
	boolean test = false;

	test = IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY | accessMask);
	EntityItemBoolType accessSystemSecurity = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	accessSystemSecurity.setValue(Boolean.toString(test));
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

	test = IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA | accessMask);
	EntityItemBoolType fileAppendData = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileAppendData.setValue(Boolean.toString(test));
	fileAppendData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileAppendData(fileAppendData);

	test = IACE.FILE_DELETE == (IACE.FILE_DELETE | accessMask);
	EntityItemBoolType fileDeleteChild = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileDeleteChild.setValue(Boolean.toString(test));
	fileDeleteChild.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileDeleteChild(fileDeleteChild);

	test = IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE | accessMask);
	EntityItemBoolType fileExecute = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileExecute.setValue(Boolean.toString(test));
	fileExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileExecute(fileExecute);

	test = IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES | accessMask);
	EntityItemBoolType fileReadAttributes = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileReadAttributes.setValue(Boolean.toString(test));
	fileReadAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadAttributes(fileReadAttributes);

	test = IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA | accessMask);
	EntityItemBoolType fileReadData = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileReadData.setValue(Boolean.toString(test));
	fileReadData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadData(fileReadData);

	test = IACE.FILE_READ_EA == (IACE.FILE_READ_EA | accessMask);
	EntityItemBoolType fileReadEa = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileReadEa.setValue(Boolean.toString(test));
	fileReadEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadEa(fileReadEa);

	test = IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES | accessMask);
	EntityItemBoolType fileWriteAttributes = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileWriteAttributes.setValue(Boolean.toString(test));
	fileWriteAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteAttributes(fileWriteAttributes);

	test = IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA | accessMask);
	EntityItemBoolType fileWriteData = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileWriteData.setValue(Boolean.toString(test));
	fileWriteData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteData(fileWriteData);

	test = IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA | accessMask);
	EntityItemBoolType fileWriteEa = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	fileWriteEa.setValue(Boolean.toString(test));
	fileWriteEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteEa(fileWriteEa);

	test = IACE.GENERIC_ALL == (IACE.GENERIC_ALL | accessMask);
	EntityItemBoolType genericAll = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	genericAll.setValue(Boolean.toString(test));
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericAll(genericAll);

	test = IACE.GENERIC_EXECUTE == (IACE.GENERIC_EXECUTE | accessMask);
	EntityItemBoolType genericExecute = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(Boolean.toString(test));
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.GENERIC_READ == (IACE.GENERIC_READ | accessMask);
	EntityItemBoolType genericRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(Boolean.toString(test));
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.GENERIC_WRITE == (IACE.GENERIC_WRITE | accessMask);
	EntityItemBoolType genericWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(Boolean.toString(test));
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.STANDARD_DELETE == (IACE.STANDARD_DELETE | accessMask);
	EntityItemBoolType standardDelete = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(Boolean.toString(test));
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.STANDARD_READ_CONTROL == (IACE.STANDARD_READ_CONTROL | accessMask);
	EntityItemBoolType standardReadControl = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(Boolean.toString(test));
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.STANDARD_SYNCHRONIZE == (IACE.STANDARD_SYNCHRONIZE | accessMask);
	EntityItemBoolType standardSynchronize = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setValue(Boolean.toString(test));
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardSynchronize(standardSynchronize);

	test = IACE.STANDARD_WRITE_DAC == (IACE.STANDARD_WRITE_DAC | accessMask);
	EntityItemBoolType standardWriteDac = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(Boolean.toString(test));
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.STANDARD_WRITE_OWNER == (IACE.STANDARD_WRITE_OWNER | accessMask);
	EntityItemBoolType standardWriteOwner = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(Boolean.toString(test));
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteOwner(standardWriteOwner);

	EntityItemStringType trusteeName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	if (directory.isBuiltinUser(p.getNetbiosName())) {
	    trusteeName.setValue(p.getName());
	} else {
	    trusteeName.setValue(p.getNetbiosName());
	}
	item.setTrusteeName(trusteeName);

	EntityItemStringType trusteeSid = JOVALSystem.factories.sc.core.createEntityItemStringType();
	trusteeSid.setValue(p.getSid());
	item.setTrusteeSid(trusteeSid);

	return JOVALSystem.factories.sc.windows.createFileeffectiverightsItem(item);
    }
}
