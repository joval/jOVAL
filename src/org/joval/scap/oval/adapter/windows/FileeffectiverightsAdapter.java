// Copyright (C) 2011 jOVAL.org.  All rights reserved.
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

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.Fileeffectiverights53Object;
import scap.oval.definitions.windows.FileeffectiverightsObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.FileeffectiverightsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Fileeffectiverights and Fileeffectiverights53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileeffectiverightsAdapter extends BaseFileAdapter<FileeffectiverightsItem> {
    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    baseInit(session);
	    classes.add(Fileeffectiverights53Object.class);
	    classes.add(FileeffectiverightsObject.class);
	} else {
	    notapplicable.add(Fileeffectiverights53Object.class);
	    notapplicable.add(FileeffectiverightsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileeffectiverightsItem.class;
    }

    protected Collection<FileeffectiverightsItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	directory = ((IWindowsSession)session).getDirectory();
	Collection<FileeffectiverightsItem> items = new ArrayList<FileeffectiverightsItem>();
	try {
	    List<IPrincipal> principals = new ArrayList<IPrincipal>();
	    IWindowsSession.View view = null;
	    boolean includeGroups = true;
	    boolean resolveGroups = false;
	    if (obj instanceof Fileeffectiverights53Object) {
		Fileeffectiverights53Object fObj = (Fileeffectiverights53Object)obj;
		view = getView(fObj.getBehaviors());
		if (fObj.isSetBehaviors()) {
		    includeGroups = fObj.getBehaviors().getIncludeGroup();
		    resolveGroups = fObj.getBehaviors().getResolveGroup();
		}
		String pSid = (String)fObj.getTrusteeSid().getValue();
		OperationEnumeration op = fObj.getTrusteeSid().getOperation();
		switch(op) {
		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(pSid);
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (p.matcher(principal.getSid()).find()) {
			    principals.add(principal);
			}
		    }
		    break;
		  }

		  case NOT_EQUAL:
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (!pSid.equals(principal.getSid())) {
			    principals.add(principal);
			}
		    }
		    break;

		  case CASE_INSENSITIVE_EQUALS:
		  case EQUALS: {
		    principals.add(directory.queryPrincipalBySid(pSid));
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } else if (obj instanceof FileeffectiverightsObject) {
		FileeffectiverightsObject fObj = (FileeffectiverightsObject)obj;
		view = getView(fObj.getBehaviors());
		if (fObj.isSetBehaviors()) {
		    includeGroups = fObj.getBehaviors().getIncludeGroup();
		    resolveGroups = fObj.getBehaviors().getResolveGroup();
		}
		String pName = (String)fObj.getTrusteeName().getValue();
		OperationEnumeration op = fObj.getTrusteeName().getOperation();
		switch(op) {
		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(pName);
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (principal.isBuiltin() && p.matcher(principal.getName()).find()) {
			    principals.add(principal);
			} else if (p.matcher(principal.getNetbiosName()).find()) {
			    principals.add(principal);
			}
		    }
		    break;
		  }

		  case NOT_EQUAL:
		    for (IPrincipal principal : directory.queryAllPrincipals()) {
			if (principal.isBuiltin() && !pName.equals(principal.getName())) {
			    principals.add(principal);
			} else if (!pName.equals(principal.getNetbiosName())) {
			    principals.add(principal);
			}
		    }
		    break;

		  case CASE_INSENSITIVE_EQUALS:
		  case EQUALS: {
		    principals.add(directory.queryPrincipal(pName));
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    }

	    //
	    // Filter out any duplicate IPrincipals
	    //
	    Map<String, IPrincipal> principalMap = new HashMap<String, IPrincipal>();
	    for (IPrincipal principal : principals) {
		principalMap.put(principal.getSid(), principal);
	    }

	    //
	    // Create items
	    //
	    for (IFile f : files) {
		FileeffectiverightsItem baseItem = (FileeffectiverightsItem)getBaseItem(obj, f);
		if (baseItem != null) {
		    for (IPrincipal principal : principalMap.values()) {
			switch(principal.getType()) {
			  case GROUP:
			    for (IPrincipal p : directory.getAllPrincipals(principal, includeGroups, resolveGroups)) {
				StringBuffer cmd = new StringBuffer("Get-EffectiveRights -ObjectType File -Name ");
				cmd.append("\"").append(f.getPath()).append("\"");
				cmd.append(" -SID ").append(principal.getSid());
				int mask = Integer.parseInt(getRunspace(view).invoke(cmd.toString()));
				items.add(makeItem(baseItem, p, mask));
			    }
			    break;

			  case USER:
			    StringBuffer cmd = new StringBuffer("Get-EffectiveRights -ObjectType File -Name ");
			    cmd.append("\"").append(f.getPath()).append("\"");
			    cmd.append(" -SID ").append(principal.getSid());
			    int mask = Integer.parseInt(getRunspace(view).invoke(cmd.toString()));
			    items.add(makeItem(baseItem, principal, mask));
			    break;
			}
		    }
		}
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
	    rc.addMessage(msg);
	} catch (PatternSyntaxException e) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    @Override
    protected List<InputStream> getPowershellAssemblies() {
	return Arrays.asList(getClass().getResourceAsStream("Effectiverights.dll"));
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Effectiverights.psm1"));
    }

    // Private

    /**
     * Create a new wrapped FileeffectiverightsItem based on the base FileeffectiverightsItem, IPrincipal and IACE.
     */
    private FileeffectiverightsItem makeItem(FileeffectiverightsItem base, IPrincipal p, int mask) throws IOException {
	FileeffectiverightsItem item = Factories.sc.windows.createFileeffectiverightsItem();
	item.setPath(base.getPath());
	item.setFilename(base.getFilename());
	item.setFilepath(base.getFilepath());
	item.setWindowsView(base.getWindowsView());

	boolean test = IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY & mask);
	EntityItemBoolType accessSystemSecurity = Factories.sc.core.createEntityItemBoolType();
	accessSystemSecurity.setValue(Boolean.toString(test));
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

	test = IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA & mask);
	EntityItemBoolType fileAppendData = Factories.sc.core.createEntityItemBoolType();
	fileAppendData.setValue(Boolean.toString(test));
	fileAppendData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileAppendData(fileAppendData);

	test = IACE.FILE_DELETE == (IACE.FILE_DELETE & mask);
	EntityItemBoolType fileDeleteChild = Factories.sc.core.createEntityItemBoolType();
	fileDeleteChild.setValue(Boolean.toString(test));
	fileDeleteChild.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileDeleteChild(fileDeleteChild);

	test = IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE & mask);
	EntityItemBoolType fileExecute = Factories.sc.core.createEntityItemBoolType();
	fileExecute.setValue(Boolean.toString(test));
	fileExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileExecute(fileExecute);

	test = IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES & mask);
	EntityItemBoolType fileReadAttributes = Factories.sc.core.createEntityItemBoolType();
	fileReadAttributes.setValue(Boolean.toString(test));
	fileReadAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadAttributes(fileReadAttributes);

	test = IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA & mask);
	EntityItemBoolType fileReadData = Factories.sc.core.createEntityItemBoolType();
	fileReadData.setValue(Boolean.toString(test));
	fileReadData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadData(fileReadData);

	test = IACE.FILE_READ_EA == (IACE.FILE_READ_EA & mask);
	EntityItemBoolType fileReadEa = Factories.sc.core.createEntityItemBoolType();
	fileReadEa.setValue(Boolean.toString(test));
	fileReadEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadEa(fileReadEa);

	test = IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES & mask);
	EntityItemBoolType fileWriteAttributes = Factories.sc.core.createEntityItemBoolType();
	fileWriteAttributes.setValue(Boolean.toString(test));
	fileWriteAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteAttributes(fileWriteAttributes);

	test = IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA & mask);
	EntityItemBoolType fileWriteData = Factories.sc.core.createEntityItemBoolType();
	fileWriteData.setValue(Boolean.toString(test));
	fileWriteData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteData(fileWriteData);

	test = IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA & mask);
	EntityItemBoolType fileWriteEa = Factories.sc.core.createEntityItemBoolType();
	fileWriteEa.setValue(Boolean.toString(test));
	fileWriteEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteEa(fileWriteEa);

	test = IACE.FILE_GENERIC_ALL == (IACE.FILE_GENERIC_ALL & mask);
	EntityItemBoolType genericAll = Factories.sc.core.createEntityItemBoolType();
	genericAll.setValue(Boolean.toString(test));
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericAll(genericAll);

	test = IACE.FILE_GENERIC_EXECUTE == (IACE.FILE_GENERIC_EXECUTE & mask);
	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(Boolean.toString(test));
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.FILE_GENERIC_READ == (IACE.FILE_GENERIC_READ & mask);
	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(Boolean.toString(test));
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.FILE_GENERIC_WRITE == (IACE.FILE_GENERIC_WRITE & mask);
	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(Boolean.toString(test));
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.DELETE == (IACE.DELETE & mask);
	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(Boolean.toString(test));
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.READ_CONTROL == (IACE.READ_CONTROL & mask);
	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(Boolean.toString(test));
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.SYNCHRONIZE == (IACE.SYNCHRONIZE & mask);
	EntityItemBoolType standardSynchronize = Factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setValue(Boolean.toString(test));
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardSynchronize(standardSynchronize);

	test = IACE.WRITE_DAC == (IACE.WRITE_DAC & mask);
	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(Boolean.toString(test));
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.WRITE_OWNER == (IACE.WRITE_OWNER & mask);
	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(Boolean.toString(test));
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteOwner(standardWriteOwner);

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
}
