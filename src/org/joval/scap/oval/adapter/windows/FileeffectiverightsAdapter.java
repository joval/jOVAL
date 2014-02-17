// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.identity.IdentityException;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.ISession;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.FileBehaviors;
import scap.oval.definitions.windows.Fileeffectiverights53Object;
import scap.oval.definitions.windows.FileeffectiverightsObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.FileeffectiverightsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Batch;
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
	    baseInit((IWindowsSession)session);
	    classes.add(Fileeffectiverights53Object.class);
	    classes.add(FileeffectiverightsObject.class);
	} else {
	    notapplicable.add(Fileeffectiverights53Object.class);
	    notapplicable.add(FileeffectiverightsObject.class);
	}
	return classes;
    }

    // Implement IBatch

    @Override
    public Collection<IResult> exec() {
	Map<IRequest, IResult> resultMap = new HashMap<IRequest, IResult>();
	Map<Integer, IRequest> requestMap = new HashMap<Integer, IRequest>();
	Map<Integer, FileeffectiverightsItem> baseItems = new HashMap<Integer, FileeffectiverightsItem>();
	try {
	    //
	    // Build a massive command...
	    //
	    StringBuffer cmd = new StringBuffer("%{");
	    String[] paths = queuePaths();
	    IFile[] files = session.getFilesystem().getFiles(paths);
	    for (int i=0; i < files.length; i++) {
		IRequest request = queue.get(i);
		Integer identifier = new Integer(i);
		requestMap.put(identifier, request);
		IRequestContext rc = request.getContext();
		ObjectType obj = request.getObject();
		ReflectedFileObject fObj = new ReflectedFileObject(obj);
		IFile f = files[i];
		if (f == null) {
		    resultMap.put(request, new Batch.Result(Collections.<FileeffectiverightsItem>emptyList(), rc));
		} else {
		    try {
			baseItems.put(identifier, (FileeffectiverightsItem)getBaseItem(obj, f));
			StringBuffer sb = new StringBuffer("\"{\";");
			sb.append("\"Object: ").append(identifier.toString()).append("\";");
			int sidNum=0;
			for (String sid : getObjectInfo(obj).principalMap.keySet()) {
			    if (sidNum++ > 0) {
				sb.append(",");
			    }
			    sb.append("\"").append(sid).append("\"");
			}
			sb.append(" | Get-EffectiveRights -ObjectType File ");
			sb.append(" -Name \"").append(f.getPath()).append("\";");
			sb.append("\"}\";");
			cmd.append(sb);
		    } catch (CollectException e) {
			resultMap.put(request, new Batch.Result(e, rc));
		    } catch (Exception e) {
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
		    }
		}
	    }
	    cmd.append("} | Transfer-Encode");
	    try {
		String data = getRunspace(((IWindowsSession)session).getNativeView()).invoke(cmd.toString());
		data = new String(Base64.decode(data), StringTools.UTF8);
		Iterator<String> iter = Arrays.asList(data.split("\r\n")).iterator();
		ObjectItems items = null;
		while ((items = nextItems(iter, requestMap, baseItems)) != null) {
		    IRequest request = requestMap.get(items.objectId);
		    resultMap.put(request, new Batch.Result(items.items, request.getContext()));
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		for (IRequest request : requestMap.values()) {
		    IRequestContext rc = request.getContext();
		    resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
		}
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    for (IRequest request : queue) {
		IRequestContext rc = request.getContext();
		resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
	    }
	}
	queue = null;
	return resultMap.values();
    }

    // Protected

    protected Class getItemClass() {
	return FileeffectiverightsItem.class;
    }

    protected Collection<FileeffectiverightsItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	Collection<FileeffectiverightsItem> items = new ArrayList<FileeffectiverightsItem>();
	try {
	    ObjectInfo info = getObjectInfo(obj);
	    for (IFile f : files) {
		FileeffectiverightsItem baseItem = (FileeffectiverightsItem)getBaseItem(obj, f);
		if (baseItem != null) {
		    StringBuffer cmd = new StringBuffer();
		    for (String sid : info.principalMap.keySet()) {
			if (cmd.length() > 0) {
			    cmd.append(",");
			}
			cmd.append("\"").append(sid).append("\"");
		    }
		    cmd.append(" | Get-EffectiveRights -ObjectType File ");
		    cmd.append(" -Name \"").append(f.getPath()).append("\"");
		    for (String line : getRunspace(info.view).invoke(cmd.toString()).split("\r\n")) {
			int ptr = line.indexOf(":");
			if (ptr != -1) {
			    String sid = line.substring(0,ptr);
			    int mask = Integer.parseInt(line.substring(ptr+1).trim());
			    items.add(makeItem(baseItem, info.principalMap.get(sid), mask));
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
	} catch (IdentityException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_IDENTITY, obj.getId(), e.getMessage()));
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
     * Associates a file path (object) with the relevant items.
     */
    static class ObjectItems {
	Collection<FileeffectiverightsItem> items;
	Integer objectId;

	ObjectItems() {
	    items = new ArrayList<FileeffectiverightsItem>();
	}
    }

    private ObjectItems nextItems(Iterator<String> input, Map<Integer, IRequest> requestMap,
		Map<Integer, FileeffectiverightsItem> baseItems) {

        boolean start = false;
        while(input.hasNext()) {
            String line = input.next();
            if (line.trim().equals("{")) {
                start = true;
                break;
            }
        }
        if (start) {
	    while(input.hasNext()) {
		String line = input.next().trim();
		if (line.startsWith("Object:")) {
		    ObjectItems items = new ObjectItems();
		    items.objectId = new Integer(line.substring(7).trim());
		    FileeffectiverightsItem baseItem = baseItems.get(items.objectId);
		    while(input.hasNext()) {
			line = input.next().trim();
			if (line.equals("}")) {
			    break;
			} else {
			    int ptr = line.indexOf(":");
			    if (ptr != -1) {
				String sid = line.substring(0,ptr);
				int mask = Integer.parseInt(line.substring(ptr+1).trim());
				try {
				    items.items.add(makeItem(baseItem, directory.queryPrincipalBySid(sid), mask));
				} catch (Exception e) {
				    IRequestContext rc = requestMap.get(items.objectId).getContext();
				    MessageType msg = Factories.common.createMessageType();
				    msg.setLevel(MessageLevelEnumeration.ERROR);
				    msg.setValue(e.getMessage());
				    rc.addMessage(msg);
				    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
				}
			    }
			}
		    }
		    return items;
		}
	    }
	}
	return null;
    }

    /**
     * Includes a map of all the IPrincipals (keyed by SID) that are relevant to the specified object, resolved according
     * to the object's behaviors.
     */
    static class ObjectInfo {
	Map<String, IPrincipal> principalMap;
	IWindowsSession.View view = null;

	ObjectInfo() {}
    }

    /**
     * Find the ObjectInfo for the specified ObjectType.
     */
    private ObjectInfo getObjectInfo(ObjectType obj) throws IdentityException, CollectException {
	directory = ((IWindowsSession)session).getDirectory();
	ObjectInfo info = new ObjectInfo();
	List<IPrincipal> principals = new ArrayList<IPrincipal>();
	boolean includeGroups = true;
	boolean resolveGroups = false;
	if (obj instanceof Fileeffectiverights53Object) {
	    Fileeffectiverights53Object fObj = (Fileeffectiverights53Object)obj;
	    info.view = getView(fObj.getBehaviors());
	    if (fObj.isSetBehaviors()) {
		includeGroups = fObj.getBehaviors().getIncludeGroup();
		resolveGroups = fObj.getBehaviors().getResolveGroup();
	    }
	    String pSid = (String)fObj.getTrusteeSid().getValue();
	    OperationEnumeration op = fObj.getTrusteeSid().getOperation();
	    switch(op) {
	      case PATTERN_MATCH: {
		Pattern p = StringTools.pattern(pSid);
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
	    info.view = getView(fObj.getBehaviors());
	    if (fObj.isSetBehaviors()) {
		includeGroups = fObj.getBehaviors().getIncludeGroup();
		resolveGroups = fObj.getBehaviors().getResolveGroup();
	    }
	    String pName = (String)fObj.getTrusteeName().getValue();
	    OperationEnumeration op = fObj.getTrusteeName().getOperation();
	    switch(op) {
	      case PATTERN_MATCH: {
		Pattern p = StringTools.pattern(pName);
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
	Map<String, IPrincipal> filtered = new HashMap<String, IPrincipal>();
	for (IPrincipal p : principals) {
	    filtered.put(p.getSid(), p);
	}
	info.principalMap = new HashMap<String, IPrincipal>();
	for (IPrincipal p : filtered.values()) {
	    switch(p.getType()) {
	      case GROUP:
		for (IPrincipal member : directory.getAllPrincipals(p, includeGroups, resolveGroups)) {
		    info.principalMap.put(member.getSid(), member);
		}
		break;
	      case USER:
		info.principalMap.put(p.getSid(), p);
		break;
	    }
	}
	return info;
    }

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
	accessSystemSecurity.setValue(test ? "1" : "0");
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

	test = IACE.FILE_APPEND_DATA == (IACE.FILE_APPEND_DATA & mask);
	EntityItemBoolType fileAppendData = Factories.sc.core.createEntityItemBoolType();
	fileAppendData.setValue(test ? "1" : "0");
	fileAppendData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileAppendData(fileAppendData);

	test = IACE.FILE_DELETE == (IACE.FILE_DELETE & mask);
	EntityItemBoolType fileDeleteChild = Factories.sc.core.createEntityItemBoolType();
	fileDeleteChild.setValue(test ? "1" : "0");
	fileDeleteChild.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileDeleteChild(fileDeleteChild);

	test = IACE.FILE_EXECUTE == (IACE.FILE_EXECUTE & mask);
	EntityItemBoolType fileExecute = Factories.sc.core.createEntityItemBoolType();
	fileExecute.setValue(test ? "1" : "0");
	fileExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileExecute(fileExecute);

	test = IACE.FILE_READ_ATTRIBUTES == (IACE.FILE_READ_ATTRIBUTES & mask);
	EntityItemBoolType fileReadAttributes = Factories.sc.core.createEntityItemBoolType();
	fileReadAttributes.setValue(test ? "1" : "0");
	fileReadAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadAttributes(fileReadAttributes);

	test = IACE.FILE_READ_DATA == (IACE.FILE_READ_DATA & mask);
	EntityItemBoolType fileReadData = Factories.sc.core.createEntityItemBoolType();
	fileReadData.setValue(test ? "1" : "0");
	fileReadData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadData(fileReadData);

	test = IACE.FILE_READ_EA == (IACE.FILE_READ_EA & mask);
	EntityItemBoolType fileReadEa = Factories.sc.core.createEntityItemBoolType();
	fileReadEa.setValue(test ? "1" : "0");
	fileReadEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileReadEa(fileReadEa);

	test = IACE.FILE_WRITE_ATTRIBUTES == (IACE.FILE_WRITE_ATTRIBUTES & mask);
	EntityItemBoolType fileWriteAttributes = Factories.sc.core.createEntityItemBoolType();
	fileWriteAttributes.setValue(test ? "1" : "0");
	fileWriteAttributes.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteAttributes(fileWriteAttributes);

	test = IACE.FILE_WRITE_DATA == (IACE.FILE_WRITE_DATA & mask);
	EntityItemBoolType fileWriteData = Factories.sc.core.createEntityItemBoolType();
	fileWriteData.setValue(test ? "1" : "0");
	fileWriteData.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteData(fileWriteData);

	test = IACE.FILE_WRITE_EA == (IACE.FILE_WRITE_EA & mask);
	EntityItemBoolType fileWriteEa = Factories.sc.core.createEntityItemBoolType();
	fileWriteEa.setValue(test ? "1" : "0");
	fileWriteEa.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setFileWriteEa(fileWriteEa);

	test = IACE.FILE_GENERIC_ALL == (IACE.FILE_GENERIC_ALL & mask);
	EntityItemBoolType genericAll = Factories.sc.core.createEntityItemBoolType();
	genericAll.setValue(test ? "1" : "0");
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericAll(genericAll);

	test = IACE.FILE_GENERIC_EXECUTE == (IACE.FILE_GENERIC_EXECUTE & mask);
	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(test ? "1" : "0");
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.FILE_GENERIC_READ == (IACE.FILE_GENERIC_READ & mask);
	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(test ? "1" : "0");
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.FILE_GENERIC_WRITE == (IACE.FILE_GENERIC_WRITE & mask);
	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(test ? "1" : "0");
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.DELETE == (IACE.DELETE & mask);
	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(test ? "1" : "0");
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.READ_CONTROL == (IACE.READ_CONTROL & mask);
	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(test ? "1" : "0");
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.SYNCHRONIZE == (IACE.SYNCHRONIZE & mask);
	EntityItemBoolType standardSynchronize = Factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setValue(test ? "1" : "0");
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardSynchronize(standardSynchronize);

	test = IACE.WRITE_DAC == (IACE.WRITE_DAC & mask);
	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(test ? "1" : "0");
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.WRITE_OWNER == (IACE.WRITE_OWNER & mask);
	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(test ? "1" : "0");
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
