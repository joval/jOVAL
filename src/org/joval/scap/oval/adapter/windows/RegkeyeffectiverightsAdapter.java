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
import oval.schemas.definitions.windows.Regkeyeffectiverights53Object;
import oval.schemas.definitions.windows.RegkeyeffectiverightsObject;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.EntityItemRegistryHiveType;
import oval.schemas.systemcharacteristics.windows.EntityItemWindowsViewType;
import oval.schemas.systemcharacteristics.windows.RegkeyeffectiverightsItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.os.windows.wmi.WmiException;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Collects items for Regkeyeffectiverights and Regkeyeffectiverights53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegkeyeffectiverightsAdapter extends BaseRegkeyAdapter<RegkeyeffectiverightsItem> {
    private IDirectory directory;
    private IRunspace runspace;
    private Map<String, List<IACE>> acls;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((IWindowsSession)session);
	    classes.add(Regkeyeffectiverights53Object.class);
	    classes.add(RegkeyeffectiverightsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return RegkeyeffectiverightsItem.class;
    }

    protected Collection<RegkeyeffectiverightsItem> getItems(ObjectType obj, ItemType base, IKey key, IRequestContext rc)
		throws Exception {

	initialize();
	Collection<RegkeyeffectiverightsItem> items = new Vector<RegkeyeffectiverightsItem>();
	RegkeyeffectiverightsItem baseItem = null;
	if (base instanceof RegkeyeffectiverightsItem) {
	    baseItem = (RegkeyeffectiverightsItem)base;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	String pSid = null, pName = null;
	boolean includeGroups = true;
	boolean resolveGroups = false;
	OperationEnumeration op = OperationEnumeration.EQUALS;
	if (obj instanceof Regkeyeffectiverights53Object) {
	    Regkeyeffectiverights53Object rObj = (Regkeyeffectiverights53Object)obj;
	    op = rObj.getTrusteeSid().getOperation();
	    pSid = (String)rObj.getTrusteeSid().getValue();
	    if (rObj.isSetBehaviors()) {
		includeGroups = rObj.getBehaviors().getIncludeGroup();
		resolveGroups = rObj.getBehaviors().getResolveGroup();
	    }
	} else if (obj instanceof RegkeyeffectiverightsObject) {
	    RegkeyeffectiverightsObject rObj = (RegkeyeffectiverightsObject)obj;
	    op = rObj.getTrusteeName().getOperation();
	    pName = (String)rObj.getTrusteeName().getValue();
	    if (rObj.isSetBehaviors()) {
		includeGroups = rObj.getBehaviors().getIncludeGroup();
		resolveGroups = rObj.getBehaviors().getResolveGroup();
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	try {
	    List<IACE> aces = getSecurity(key);
	    switch(op) {
	      case PATTERN_MATCH:
		Pattern p = null;
		if (pSid == null) {
		    p = Pattern.compile(pName);
		} else {
		    p = Pattern.compile(pSid);
		}
		for (IACE ace : aces) {
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
		for (IPrincipal principal : principals) {
		    for (IACE ace : aces) {
			switch(op) {
			  case EQUALS:
			  case CASE_INSENSITIVE_EQUALS:
			    if (directory.isApplicable(principal, ace)) {
				items.add(makeItem(baseItem, principal, ace));
			    }
			    break;
			  case NOT_EQUAL:
			    if (!directory.isApplicable(principal, ace)) {
				items.add(makeItem(baseItem, principal, ace));
			    }
			    break;
			}
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
	} catch (IOException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_REGDACL, key.toString(), e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (PowershellException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_REGDACL, key.toString(), e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    /**
     * Create a new RegkeyeffectiverightsItem based on the base RegkeyeffectiverightsItem, IPrincipal and IACE.
     */
    private RegkeyeffectiverightsItem makeItem(RegkeyeffectiverightsItem base, IPrincipal p, IACE ace) {
	RegkeyeffectiverightsItem item = Factories.sc.windows.createRegkeyeffectiverightsItem();
	item.setHive(base.getHive());
	item.setKey(base.getKey());
	if (base.isSetWindowsView()) {
	    item.setWindowsView(base.getWindowsView());
	}

	int accessMask = ace.getAccessMask();
	boolean test = false;

	test = IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY & accessMask);
	EntityItemBoolType accessSystemSecurity = Factories.sc.core.createEntityItemBoolType();
	accessSystemSecurity.setValue(Boolean.toString(test));
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

	test = IACE.GENERIC_ALL == (IACE.GENERIC_ALL & accessMask);
	EntityItemBoolType genericAll = Factories.sc.core.createEntityItemBoolType();
	genericAll.setValue(Boolean.toString(test));
	genericAll.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericAll(genericAll);

	test = IACE.GENERIC_EXECUTE == (IACE.GENERIC_EXECUTE & accessMask);
	EntityItemBoolType genericExecute = Factories.sc.core.createEntityItemBoolType();
	genericExecute.setValue(Boolean.toString(test));
	genericExecute.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericExecute(genericExecute);

	test = IACE.GENERIC_READ == (IACE.GENERIC_READ & accessMask);
	EntityItemBoolType genericRead = Factories.sc.core.createEntityItemBoolType();
	genericRead.setValue(Boolean.toString(test));
	genericRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericRead(genericRead);

	test = IACE.GENERIC_WRITE == (IACE.GENERIC_WRITE & accessMask);
	EntityItemBoolType genericWrite = Factories.sc.core.createEntityItemBoolType();
	genericWrite.setValue(Boolean.toString(test));
	genericWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGenericWrite(genericWrite);

	test = IACE.STANDARD_DELETE == (IACE.STANDARD_DELETE & accessMask);
	EntityItemBoolType standardDelete = Factories.sc.core.createEntityItemBoolType();
	standardDelete.setValue(Boolean.toString(test));
	standardDelete.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardDelete(standardDelete);

	test = IACE.STANDARD_READ_CONTROL == (IACE.STANDARD_READ_CONTROL & accessMask);
	EntityItemBoolType standardReadControl = Factories.sc.core.createEntityItemBoolType();
	standardReadControl.setValue(Boolean.toString(test));
	standardReadControl.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardReadControl(standardReadControl);

	test = IACE.STANDARD_SYNCHRONIZE == (IACE.STANDARD_SYNCHRONIZE & accessMask);
	EntityItemBoolType standardSynchronize = Factories.sc.core.createEntityItemBoolType();
	standardSynchronize.setValue(Boolean.toString(test));
	standardSynchronize.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardSynchronize(standardSynchronize);

	test = IACE.STANDARD_WRITE_DAC == (IACE.STANDARD_WRITE_DAC & accessMask);
	EntityItemBoolType standardWriteDac = Factories.sc.core.createEntityItemBoolType();
	standardWriteDac.setValue(Boolean.toString(test));
	standardWriteDac.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteDac(standardWriteDac);

	test = IACE.STANDARD_WRITE_OWNER == (IACE.STANDARD_WRITE_OWNER & accessMask);
	EntityItemBoolType standardWriteOwner = Factories.sc.core.createEntityItemBoolType();
	standardWriteOwner.setValue(Boolean.toString(test));
	standardWriteOwner.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setStandardWriteOwner(standardWriteOwner);

	test = IACE.KEY_CREATE_LINK == (IACE.KEY_CREATE_LINK & accessMask);
	EntityItemBoolType keyCreateLink = Factories.sc.core.createEntityItemBoolType();
	keyCreateLink.setValue(Boolean.toString(test));
	keyCreateLink.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyCreateLink(keyCreateLink);

	test = IACE.KEY_CREATE_SUB_KEY == (IACE.KEY_CREATE_SUB_KEY & accessMask);
	EntityItemBoolType keyCreateSubKey = Factories.sc.core.createEntityItemBoolType();
	keyCreateSubKey.setValue(Boolean.toString(test));
	keyCreateSubKey.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyCreateSubKey(keyCreateSubKey);

	test = IACE.KEY_ENUMERATE_SUB_KEYS == (IACE.KEY_ENUMERATE_SUB_KEYS & accessMask);
	EntityItemBoolType keyEnumerateSubKeys = Factories.sc.core.createEntityItemBoolType();
	keyEnumerateSubKeys.setValue(Boolean.toString(test));
	keyEnumerateSubKeys.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyEnumerateSubKeys(keyEnumerateSubKeys);

	test = IACE.KEY_NOTIFY == (IACE.KEY_NOTIFY & accessMask);
	EntityItemBoolType keyNotify = Factories.sc.core.createEntityItemBoolType();
	keyNotify.setValue(Boolean.toString(test));
	keyNotify.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyNotify(keyNotify);

	test = IACE.KEY_QUERY_VALUE == (IACE.KEY_QUERY_VALUE & accessMask);
	EntityItemBoolType keyQueryValue = Factories.sc.core.createEntityItemBoolType();
	keyQueryValue.setValue(Boolean.toString(test));
	keyQueryValue.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyQueryValue(keyQueryValue);

	test = IACE.KEY_SET_VALUE == (IACE.KEY_SET_VALUE & accessMask);
	EntityItemBoolType keySetValue = Factories.sc.core.createEntityItemBoolType();
	keySetValue.setValue(Boolean.toString(test));
	keySetValue.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeySetValue(keySetValue);

	test = IACE.KEY_WOW64_32_KEY == (IACE.KEY_WOW64_32_KEY & accessMask);
	EntityItemBoolType keyWow6432Key = Factories.sc.core.createEntityItemBoolType();
	keyWow6432Key.setValue(Boolean.toString(test));
	keyWow6432Key.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyWow6432Key(keyWow6432Key);

	test = IACE.KEY_WOW64_64_KEY == (IACE.KEY_WOW64_64_KEY & accessMask);
	EntityItemBoolType keyWow6464Key = Factories.sc.core.createEntityItemBoolType();
	keyWow6464Key.setValue(Boolean.toString(test));
	keyWow6464Key.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyWow6464Key(keyWow6464Key);

	test = IACE.KEY_WOW64_RES == (IACE.KEY_WOW64_RES & accessMask);
	EntityItemBoolType keyWow64Res = Factories.sc.core.createEntityItemBoolType();
	keyWow64Res.setValue(Boolean.toString(test));
	keyWow64Res.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyWow64Res(keyWow64Res);

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

    /**
     * Idempotent (well, harmless anyway)
     */
    private void initialize() {
	//
	// Always grab a fresh directory in case there's been a reconnect since initialization.
	//
	directory = session.getDirectory();

	if (acls == null) {
	    acls = new HashMap<String, List<IACE>>();
	} else {
	    return; // previously initialized
	}

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Cmdlet utilities
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
		runspace.loadModule(getClass().getResourceAsStream("Regkeyeffectiverights.psm1"));
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Retrieve the access entries for the file.
     */
    private List<IACE> getSecurity(IKey key) throws IOException, NoSuchElementException, PowershellException {
	String path = key.toString().toUpperCase();
	if (acls.containsKey(path)) {
	    return acls.get(path);
	} else {
	    acls.put(path, new ArrayList<IACE>());
	    HashMap<String, IACE> aces = new HashMap<String, IACE>();
	    String pathArg = key.toString();
	    if (path.indexOf(" ") != -1) {
		if (!path.startsWith("\"") && !path.endsWith("\"")) {
		    pathArg = new StringBuffer("\"").append(pathArg).append("\"").toString();
		}
	    }
	    String data = runspace.invoke("Get-RegkeyEffectiveRights " + pathArg);
	    if (data != null) {
		for (String entry : data.split("\r\n")) {
		    int ptr = entry.indexOf(":");
		    String sid = entry.substring(0,ptr).trim();
		    int mask = Integer.valueOf(entry.substring(ptr+1).trim());
		    if (aces.containsKey(sid)) {
			aces.put(sid, new ACE(sid, aces.get(sid).getAccessMask() | mask));
		    } else {
			aces.put(sid, new ACE(sid, mask));
		    }
		}
	    }
	    acls.get(path).addAll(aces.values());
	    return acls.get(path);
	}
    }

    class ACE implements IACE {
	private String sid;
	private int mask;

	ACE(String sid, int mask) {
	    this.sid = sid;
	    this.mask = mask;
	}

	public int getFlags() {
	    return 0;
	}

	public int getAccessMask() {
	    return mask;
	}

	public String getSid() {
	    return sid;
	}
    }
}
