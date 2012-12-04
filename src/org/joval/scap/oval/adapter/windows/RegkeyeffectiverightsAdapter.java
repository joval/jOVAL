// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.registry.IKey;
import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.registry.IRegistry;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.identity.ACE;
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

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((IWindowsSession)session);
	    directory = this.session.getDirectory();
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
	//
	// Map the Key's hive to one of the names supported by the GetNamedSecurityInfo method.
	//
	// See: http://msdn.microsoft.com/en-us/library/windows/desktop/aa379593%28v=vs.85%29.aspx
	//
	String hive = null;
	if (IRegistry.HKLM.equals(key.getHive())) {
	    hive = "MACHINE";
	} else if (IRegistry.HKU.equals(key.getHive())) {
	    hive = "USERS";
	} else if (IRegistry.HKCU.equals(key.getHive())) {
	    hive = "CURRENT_USER";
	} else if (IRegistry.HKCR.equals(key.getHive())) {
	    hive = "CLASSES_ROOT";
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_WINREG_HIVE_NAME, key.getHive());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	Collection<RegkeyeffectiverightsItem> items = new ArrayList<RegkeyeffectiverightsItem>();
	RegkeyeffectiverightsItem baseItem = (RegkeyeffectiverightsItem)base;
	try {
	    List<IPrincipal> principals = new ArrayList<IPrincipal>();
	    IWindowsSession.View view = null;
	    boolean includeGroups = true;
	    boolean resolveGroups = false;
	    if (obj instanceof Regkeyeffectiverights53Object) {
		Regkeyeffectiverights53Object rObj = (Regkeyeffectiverights53Object)obj;
		view = getView(rObj.getBehaviors());
		if (rObj.isSetBehaviors()) {
		    includeGroups = rObj.getBehaviors().getIncludeGroup();
		    resolveGroups = rObj.getBehaviors().getResolveGroup();
		}
		String pSid = (String)rObj.getTrusteeSid().getValue();
		OperationEnumeration op = rObj.getTrusteeSid().getOperation();
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
	    } else if (obj instanceof RegkeyeffectiverightsObject) {
		RegkeyeffectiverightsObject rObj = (RegkeyeffectiverightsObject)obj;
		view = getView(rObj.getBehaviors());
		if (rObj.isSetBehaviors()) {
		    includeGroups = rObj.getBehaviors().getIncludeGroup();
		    resolveGroups = rObj.getBehaviors().getResolveGroup();
		}
		String pName = (String)rObj.getTrusteeName().getValue();
		OperationEnumeration op = rObj.getTrusteeName().getOperation();
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
	    for (IPrincipal principal : principalMap.values()) {
		switch(principal.getType()) {
		  case USER: {
		    StringBuffer cmd = new StringBuffer("Get-EffectiveRights -ObjectType RegKey -Path ");
		    cmd.append("\"").append(hive).append("\\").append(key.getPath()).append("\"");
		    cmd.append(" -SID ").append(principal.getSid());
		    int mask = Integer.parseInt(getRunspace(view).invoke(cmd.toString()));
		    items.add(makeItem(baseItem, principal, mask));
		    break;
		  }
		  case GROUP:
		    for (IPrincipal p : directory.getAllPrincipals(principal, includeGroups, resolveGroups)) {
			StringBuffer cmd = new StringBuffer("Get-EffectiveRights -ObjectType RegKey -Path ");
			cmd.append("\"").append(hive).append("\\").append(key.getPath()).append("\"");
			cmd.append(" -SID ").append(principal.getSid());
			int mask = Integer.parseInt(getRunspace(view).invoke(cmd.toString()));
			items.add(makeItem(baseItem, p, mask));
		    }
		    break;
		}
	    }
	} catch (NoSuchElementException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_NOPRINCIPAL, e.getMessage()));
	    rc.addMessage(msg);
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
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Effectiverights.psm1"));
    }

    // Private

    /**
     * Create a new RegkeyeffectiverightsItem based on the base RegkeyeffectiverightsItem, IPrincipal and mask.
     */
    private RegkeyeffectiverightsItem makeItem(RegkeyeffectiverightsItem base, IPrincipal p, int mask) {
	RegkeyeffectiverightsItem item = Factories.sc.windows.createRegkeyeffectiverightsItem();
	item.setHive(base.getHive());
	item.setKey(base.getKey());
	if (base.isSetWindowsView()) {
	    item.setWindowsView(base.getWindowsView());
	}

	boolean test = IACE.ACCESS_SYSTEM_SECURITY == (IACE.ACCESS_SYSTEM_SECURITY & mask);
	EntityItemBoolType accessSystemSecurity = Factories.sc.core.createEntityItemBoolType();
	accessSystemSecurity.setValue(Boolean.toString(test));
	accessSystemSecurity.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setAccessSystemSecurity(accessSystemSecurity);

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

	test = IACE.KEY_CREATE_LINK == (IACE.KEY_CREATE_LINK & mask);
	EntityItemBoolType keyCreateLink = Factories.sc.core.createEntityItemBoolType();
	keyCreateLink.setValue(Boolean.toString(test));
	keyCreateLink.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyCreateLink(keyCreateLink);

	test = IACE.KEY_CREATE_SUB_KEY == (IACE.KEY_CREATE_SUB_KEY & mask);
	EntityItemBoolType keyCreateSubKey = Factories.sc.core.createEntityItemBoolType();
	keyCreateSubKey.setValue(Boolean.toString(test));
	keyCreateSubKey.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyCreateSubKey(keyCreateSubKey);

	test = IACE.KEY_ENUMERATE_SUB_KEYS == (IACE.KEY_ENUMERATE_SUB_KEYS & mask);
	EntityItemBoolType keyEnumerateSubKeys = Factories.sc.core.createEntityItemBoolType();
	keyEnumerateSubKeys.setValue(Boolean.toString(test));
	keyEnumerateSubKeys.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyEnumerateSubKeys(keyEnumerateSubKeys);

	test = IACE.KEY_NOTIFY == (IACE.KEY_NOTIFY & mask);
	EntityItemBoolType keyNotify = Factories.sc.core.createEntityItemBoolType();
	keyNotify.setValue(Boolean.toString(test));
	keyNotify.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyNotify(keyNotify);

	test = IACE.KEY_QUERY_VALUE == (IACE.KEY_QUERY_VALUE & mask);
	EntityItemBoolType keyQueryValue = Factories.sc.core.createEntityItemBoolType();
	keyQueryValue.setValue(Boolean.toString(test));
	keyQueryValue.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyQueryValue(keyQueryValue);

	test = IACE.KEY_SET_VALUE == (IACE.KEY_SET_VALUE & mask);
	EntityItemBoolType keySetValue = Factories.sc.core.createEntityItemBoolType();
	keySetValue.setValue(Boolean.toString(test));
	keySetValue.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeySetValue(keySetValue);

	test = IACE.KEY_WOW64_32_KEY == (IACE.KEY_WOW64_32_KEY & mask);
	EntityItemBoolType keyWow6432Key = Factories.sc.core.createEntityItemBoolType();
	keyWow6432Key.setValue(Boolean.toString(test));
	keyWow6432Key.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyWow6432Key(keyWow6432Key);

	test = IACE.KEY_WOW64_64_KEY == (IACE.KEY_WOW64_64_KEY & mask);
	EntityItemBoolType keyWow6464Key = Factories.sc.core.createEntityItemBoolType();
	keyWow6464Key.setValue(Boolean.toString(test));
	keyWow6464Key.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setKeyWow6464Key(keyWow6464Key);

	test = IACE.KEY_WOW64_RES == (IACE.KEY_WOW64_RES & mask);
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
}
