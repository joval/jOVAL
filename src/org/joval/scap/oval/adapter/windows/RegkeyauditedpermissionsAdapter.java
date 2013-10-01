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
import jsaf.intf.windows.registry.IKey;
import jsaf.intf.windows.identity.IACE;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.Regkeyauditedpermissions53Object;
import scap.oval.definitions.windows.RegkeyauditedpermissionsObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.EntityItemAuditType;
import scap.oval.systemcharacteristics.windows.RegkeyauditedpermissionsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Collects items for Regkeyauditedpermissions and Regkeyauditedpermissions53 objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegkeyauditedpermissionsAdapter extends BaseRegkeyAdapter<RegkeyauditedpermissionsItem> {
    public static final int SUCCESSFUL_ACCESS_ACE_FLAG	= 64;
    public static final int FAILED_ACCESS_ACE_FLAG	= 128;

    private IDirectory directory;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    super.init((IWindowsSession)session);
	    classes.add(Regkeyauditedpermissions53Object.class);
	    classes.add(RegkeyauditedpermissionsObject.class);
	} else {
	    notapplicable.add(Regkeyauditedpermissions53Object.class);
	    notapplicable.add(RegkeyauditedpermissionsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return RegkeyauditedpermissionsItem.class;
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Auditedpermissions.psm1"));
    }

    protected Collection<RegkeyauditedpermissionsItem> getItems(ObjectType obj, Collection<IKey> keys, IRequestContext rc)
		throws CollectException {

	directory = session.getDirectory();
	String pSid = null, pName = null;
	boolean ig = true;  // INCLUDE GROUPS
	boolean rg = false; // RESOLVE GROUPS
	OperationEnumeration op = OperationEnumeration.EQUALS;
	IWindowsSession.View view = session.getNativeView();
	if (obj instanceof Regkeyauditedpermissions53Object) {
	    Regkeyauditedpermissions53Object rObj = (Regkeyauditedpermissions53Object)obj;
	    op = rObj.getTrusteeSid().getOperation();
	    pSid = (String)rObj.getTrusteeSid().getValue();
	    if (rObj.isSetBehaviors()) {
		view = getView(rObj.getBehaviors());
		ig = rObj.getBehaviors().getIncludeGroup();
		rg = rObj.getBehaviors().getResolveGroup();
	    }
	} else if (obj instanceof RegkeyauditedpermissionsObject) {
	    RegkeyauditedpermissionsObject rObj = (RegkeyauditedpermissionsObject)obj;
	    op = rObj.getTrusteeName().getOperation();
	    pName = (String)rObj.getTrusteeName().getValue();
	    if (rObj.isSetBehaviors()) {
		view = getView(rObj.getBehaviors());
		ig = rObj.getBehaviors().getIncludeGroup();
		rg = rObj.getBehaviors().getResolveGroup();
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.ERROR);
	}

	Collection<RegkeyauditedpermissionsItem> items = new ArrayList<RegkeyauditedpermissionsItem>();
	try {
	    Map<String, Map<String, List<AuditRule>>> auditRuleMap = getAuditRules(keys, view);
	    for (IKey key : keys) {
		RegkeyauditedpermissionsItem baseItem = (RegkeyauditedpermissionsItem)getBaseItem(obj, key);
		Map<String, List<AuditRule>> rules = auditRuleMap.get(key.toString());
		switch(op) {
		  case PATTERN_MATCH:
		    Pattern p = null;
		    if (pSid == null) {
			p = StringTools.pattern(pName);
		    } else {
			p = StringTools.pattern(pSid);
		    }
		    //
		    // Note: per the specification, the scope is limited to the trustees referenced by the security
		    // descriptor, as opposed to the full scope of all known trustees.
		    //
		    for (Map.Entry<String, List<AuditRule>> entry : rules.entrySet()) {
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
		    for (IPrincipal principal : principals) {
			switch(op) {
			  case EQUALS:
			  case CASE_INSENSITIVE_EQUALS:
			    items.add(makeItem(baseItem, principal, rules.get(principal.getSid())));
			    break;
			  case NOT_EQUAL:
			    items.add(makeItem(baseItem, principal, rules.get(principal.getSid())));
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
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    /**
     * Retrieve the access entries for the specified files.
     */
    private Map<String, Map<String, List<AuditRule>>> getAuditRules(Collection<IKey> keys, IWindowsSession.View view)
		throws Exception {

	StringBuffer cmd = new StringBuffer();
	for (IKey key : keys) {
	    if (cmd.length() > 0) {
		cmd.append(",");
	    }
	    cmd.append("\"Registry::").append(key.toString()).append("\"");
	}
	cmd.append(" | Get-Item | Get-AuditedPermissions | Transfer-Encode");

	Map<String, Map<String, List<AuditRule>>> result = new HashMap<String, Map<String, List<AuditRule>>>();
	byte[] data = Base64.decode(getRunspace(view).invoke(cmd.toString()));
	Iterator<String> lines = Arrays.asList(new String(data, StringTools.UTF8).split("\r\n")).iterator();
	KeyAuditRules keyAuditRules = null;
	while ((keyAuditRules = nextKeyAuditRules(lines)) != null) {
	    result.put(keyAuditRules.getPath(), keyAuditRules.getRuleMap());
	}
	return result;
    }

    static final String OPEN = "{";
    static final String CLOSE = "}";

    private KeyAuditRules nextKeyAuditRules(Iterator<String> lines) {
	KeyAuditRules keyAuditRules = null;
	while(lines.hasNext()) {
	    String line = lines.next();
	    if (line.equals(OPEN)) {
		if (lines.hasNext()) {
		    line = lines.next();
		    if (line.startsWith("Path: ")) {
			keyAuditRules = new KeyAuditRules(line.substring(6));
			while(lines.hasNext()) {
	 		    line = lines.next();
			    if (line.equals(CLOSE)) {
				return keyAuditRules;
			    } else {
		        	int ptr1 = line.indexOf(":");
		        	int ptr2 = line.indexOf(",");
		        	String sid = line.substring(0,ptr1).trim();
		        	int mask = Integer.valueOf(line.substring(ptr1+1,ptr2).trim());
		        	int flags = Integer.valueOf(line.substring(ptr2+1).trim());
		        	keyAuditRules.addRule(sid, new AuditRule(sid, mask, flags));
			    }
			}
		    }
		}
	    }
	}
	return keyAuditRules;
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
     * Create a new wrapped RegkeyauditedpermissionsItem based on the base RegkeyauditedpermissionsItem, IPrincipal and mask.
     */
    private RegkeyauditedpermissionsItem makeItem(RegkeyauditedpermissionsItem base, IPrincipal p, List<AuditRule> rules) {
	RegkeyauditedpermissionsItem item = Factories.sc.windows.createRegkeyauditedpermissionsItem();
	item.setHive(base.getHive());
	item.setKey(base.getKey());
	item.setWindowsView(base.getWindowsView());

	//
	// By default, set all the permissions to none.
	//
	EntityItemAuditType audit_none = Factories.sc.windows.createEntityItemAuditType();
	audit_none.setValue("AUDIT_NONE");
	item.setAccessSystemSecurity(audit_none);
	item.setKeyQueryValue(audit_none);
	item.setKeySetValue(audit_none);
	item.setKeyCreateSubKey(audit_none);
	item.setKeyEnumerateSubKeys(audit_none);
	item.setKeyNotify(audit_none);
	item.setKeyCreateLink(audit_none);
	item.setKeyWow6464Key(audit_none);
	item.setKeyWow6432Key(audit_none);
	item.setKeyWow64Res(audit_none);
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
		if (IACE.KEY_QUERY_VALUE == (IACE.KEY_QUERY_VALUE & rule.getAccessMask())) {
		    EntityItemAuditType keyQueryValue = Factories.sc.windows.createEntityItemAuditType();
		    keyQueryValue.setValue(toAuditValue(rule));
		    item.setKeyQueryValue(keyQueryValue);
		}
		if (IACE.KEY_SET_VALUE == (IACE.KEY_SET_VALUE & rule.getAccessMask())) {
		    EntityItemAuditType keySetValue = Factories.sc.windows.createEntityItemAuditType();
		    keySetValue.setValue(toAuditValue(rule));
		    item.setKeySetValue(keySetValue);
		}
		if (IACE.KEY_CREATE_SUB_KEY == (IACE.KEY_CREATE_SUB_KEY & rule.getAccessMask())) {
		    EntityItemAuditType keyCreateSubKey = Factories.sc.windows.createEntityItemAuditType();
		    keyCreateSubKey.setValue(toAuditValue(rule));
		    item.setKeyCreateSubKey(keyCreateSubKey);
		}
		if (IACE.KEY_ENUMERATE_SUB_KEYS == (IACE.KEY_ENUMERATE_SUB_KEYS & rule.getAccessMask())) {
		    EntityItemAuditType keyEnumerateSubKeys = Factories.sc.windows.createEntityItemAuditType();
		    keyEnumerateSubKeys.setValue(toAuditValue(rule));
		    item.setKeyEnumerateSubKeys(keyEnumerateSubKeys);
		}
		if (IACE.KEY_NOTIFY == (IACE.KEY_NOTIFY & rule.getAccessMask())) {
		    EntityItemAuditType keyNotify = Factories.sc.windows.createEntityItemAuditType();
		    keyNotify.setValue(toAuditValue(rule));
		    item.setKeyNotify(keyNotify);
		}
		if (IACE.KEY_CREATE_LINK == (IACE.KEY_CREATE_LINK & rule.getAccessMask())) {
		    EntityItemAuditType keyCreateLink = Factories.sc.windows.createEntityItemAuditType();
		    keyCreateLink.setValue(toAuditValue(rule));
		    item.setKeyCreateLink(keyCreateLink);
		}
		if (IACE.KEY_WOW64_64_KEY == (IACE.KEY_WOW64_64_KEY & rule.getAccessMask())) {
		    EntityItemAuditType keyWow6464Key = Factories.sc.windows.createEntityItemAuditType();
		    keyWow6464Key.setValue(toAuditValue(rule));
		    item.setKeyWow6464Key(keyWow6464Key);
		}
		if (IACE.KEY_WOW64_32_KEY == (IACE.KEY_WOW64_32_KEY & rule.getAccessMask())) {
		    EntityItemAuditType keyWow6432Key = Factories.sc.windows.createEntityItemAuditType();
		    keyWow6432Key.setValue(toAuditValue(rule));
		    item.setKeyWow6432Key(keyWow6432Key);
		}
		if (IACE.KEY_WOW64_RES == (IACE.KEY_WOW64_RES & rule.getAccessMask())) {
		    EntityItemAuditType keyWow64Res = Factories.sc.windows.createEntityItemAuditType();
		    keyWow64Res.setValue(toAuditValue(rule));
		    item.setKeyWow64Res(keyWow64Res);
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

    class KeyAuditRules {
	private String path;
	private Map<String, List<AuditRule>> ruleMap;

	KeyAuditRules(String path) {
	    this.path = path;
	    ruleMap = new HashMap<String, List<AuditRule>>();
	}

	void addRule(String sid, AuditRule rule) {
	    if (!ruleMap.containsKey(sid)) {
		ruleMap.put(sid, new ArrayList<AuditRule>());
	    }
	    ruleMap.get(sid).add(rule);
	}

	Map<String, List<AuditRule>> getRuleMap() {
	    return ruleMap;
	}

	String getPath() {
	    return path;
	}
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
