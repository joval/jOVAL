// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.AccesstokenBehaviors;
import oval.schemas.definitions.windows.AccesstokenObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.AccesstokenItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.powershell.IRunspace;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.powershell.PowershellException;
import org.joval.os.windows.wmi.WmiException;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:accesstoken_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AccesstokenAdapter implements IAdapter {
    private IWindowsSession session;
    private IRunspace runspace;
    private IDirectory directory;
    private Hashtable<String, AccesstokenItem> itemCache;
    private Hashtable<String, MessageType> errors;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(AccesstokenObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	directory = session.getDirectory();
	if (itemCache == null) {
	    init();
	}
	if (runspace == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL), FlagEnumeration.NOT_COLLECTED);
	}
	Hashtable<String, AccesstokenItem> items = new Hashtable<String, AccesstokenItem>();

	AccesstokenObject aObj = (AccesstokenObject)obj;
	boolean include = true;
	boolean resolve = false;
	if (aObj.isSetBehaviors()) {
	    AccesstokenBehaviors behaviors = aObj.getBehaviors();
	    include = behaviors.getIncludeGroup();
	    resolve = behaviors.getResolveGroup();
	}
	String principalStr = (String)aObj.getSecurityPrincipal().getValue();
	OperationEnumeration op = aObj.getSecurityPrincipal().getOperation();
	try {
	    Collection<IPrincipal> principals = new Vector<IPrincipal>();
	    switch(op) {
	      case EQUALS:
		principals.add(directory.queryPrincipal(principalStr));
		break;

	      case NOT_EQUAL:
	      case PATTERN_MATCH:
		Collection<IPrincipal> allPrincipals = new Vector<IPrincipal>();
		allPrincipals.addAll(directory.queryAllUsers());
		allPrincipals.addAll(directory.queryAllGroups());
		if (op == OperationEnumeration.NOT_EQUAL) {
		    for (IPrincipal p : allPrincipals) {
			if (!getCanonicalizedPrincipalName(p).equals(principalStr)) {
			    principals.add(p);
			}
		    }
		} else {
		    Pattern pattern = Pattern.compile(principalStr);
		    for (IPrincipal p : allPrincipals) {
			Matcher m = pattern.matcher(getCanonicalizedPrincipalName(p));
			if (m.find()) {
			    principals.add(p);
			}
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    for (IPrincipal principal : principals) {
		for (IPrincipal p : directory.getAllPrincipals(principal, include, resolve)) {
		    String sid = principal.getSid();
		    if (errors.containsKey(sid)) {
			rc.addMessage(errors.get(sid));
		    } else if (itemCache.containsKey(sid)) {
			items.put(sid, itemCache.get(sid));
		    } else {
			try {
			    AccesstokenItem item = makeItem(p);
			    items.put(sid, item);
			    itemCache.put(sid, item);
			} catch (Exception e) {
			    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    MessageType msg = Factories.common.createMessageType();
			    msg.setLevel(MessageLevelEnumeration.ERROR);
			    msg.setValue(e.getMessage());
			    rc.addMessage(msg);
			    errors.put(sid, msg);
			}
		    }
		}
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalArgumentException e) {
	    // Domain was not found
	} catch (NoSuchElementException e) {
	    // No match
	} catch (WmiException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_WINWMI_GENERAL, obj.getId(), e.getMessage()));
	    rc.addMessage(msg);
	}
	return items.values();
    }

    // Private

    private static final EntityItemBoolType TRUE;
    private static final EntityItemBoolType FALSE;
    static {
	EntityItemBoolType tempT = Factories.sc.core.createEntityItemBoolType();
	tempT.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	tempT.setValue("1");
	TRUE = tempT;

	EntityItemBoolType tempF = Factories.sc.core.createEntityItemBoolType();
	tempF.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	tempF.setValue("0");
	FALSE = tempF;
    }

    private AccesstokenItem makeItem(IPrincipal principal) throws Exception {
	AccesstokenItem item = Factories.sc.windows.createAccesstokenItem();
	EntityItemStringType principalType = Factories.sc.core.createEntityItemStringType();
	String principalName = getCanonicalizedPrincipalName(principal);
	principalType.setValue(principalName);
	item.setSecurityPrincipal(principalType);
	session.getLogger().debug(JOVALMsg.STATUS_WIN_ACCESSTOKEN, principalName);
	String data = null;
	try {
	    data = runspace.invoke("Get-AccessTokens \"" + principal.getSid() + "\"");
	} catch (PowershellException e) {
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_ACCESSTOKEN_PRINCIPAL, principalName, e.getMessage());
	    session.getLogger().warn(s);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(s);
	    item.getMessage().add(msg);
	    item.setStatus(StatusEnumeration.ERROR);
	}
	if (data != null) {
	    for (String line : data.split("\n")) {
		String privilege = line.trim();
		if ("seassignprimarytokenprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeassignprimarytokenprivilege(TRUE);
		} else if ("seauditprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeauditprivilege(TRUE);
		} else if ("sebackupprivilege".equalsIgnoreCase(privilege)) {
		    item.setSebackupprivilege(TRUE);
		} else if ("sechangenotifyprivilege".equalsIgnoreCase(privilege)) {
		    item.setSechangenotifyprivilege(TRUE);
		} else if ("secreateglobalprivilege".equalsIgnoreCase(privilege)) {
		    item.setSecreateglobalprivilege(TRUE);
		} else if ("secreatepagefileprivilege".equalsIgnoreCase(privilege)) {
		    item.setSecreatepagefileprivilege(TRUE);
		} else if ("secreatepermanentprivilege".equalsIgnoreCase(privilege)) {
		    item.setSecreatepermanentprivilege(TRUE);
		} else if ("secreatesymboliclinkprivilege".equalsIgnoreCase(privilege)) {
		    item.setSecreatesymboliclinkprivilege(TRUE);
		} else if ("secreatetokenprivilege".equalsIgnoreCase(privilege)) {
		    item.setSecreatetokenprivilege(TRUE);
		} else if ("sedebugprivilege".equalsIgnoreCase(privilege)) {
		    item.setSedebugprivilege(TRUE);
		} else if ("seenabledelegationprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeenabledelegationprivilege(TRUE);
		} else if ("seimpersonateprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeimpersonateprivilege(TRUE);
		} else if ("seincreasebasepriorityprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeincreasebasepriorityprivilege(TRUE);
		} else if ("seincreasequotaprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeincreasequotaprivilege(TRUE);
		} else if ("seincreaseworkingsetprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeincreaseworkingsetprivilege(TRUE);
		} else if ("seloaddriverprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeloaddriverprivilege(TRUE);
		} else if ("selockmemoryprivilege".equalsIgnoreCase(privilege)) {
		    item.setSelockmemoryprivilege(TRUE);
		} else if ("semachineaccountprivilege".equalsIgnoreCase(privilege)) {
		    item.setSemachineaccountprivilege(TRUE);
		} else if ("semanagevolumeprivilege".equalsIgnoreCase(privilege)) {
		    item.setSemanagevolumeprivilege(TRUE);
		} else if ("seprofilesingleprocessprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeprofilesingleprocessprivilege(TRUE);
		} else if ("serelabelprivilege".equalsIgnoreCase(privilege)) {
		    item.setSerelabelprivilege(TRUE);
		} else if ("seremoteshutdownprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeremoteshutdownprivilege(TRUE);
		} else if ("serestoreprivilege".equalsIgnoreCase(privilege)) {
		    item.setSerestoreprivilege(TRUE);
		} else if ("sesecurityprivilege".equalsIgnoreCase(privilege)) {
		    item.setSesecurityprivilege(TRUE);
		} else if ("seshutdownprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeshutdownprivilege(TRUE);
		} else if ("sesyncagentprivilege".equalsIgnoreCase(privilege)) {
		    item.setSesyncagentprivilege(TRUE);
		} else if ("sesystemenvironmentprivilege".equalsIgnoreCase(privilege)) {
		    item.setSesystemenvironmentprivilege(TRUE);
		} else if ("sesystemprofileprivilege".equalsIgnoreCase(privilege)) {
		    item.setSesystemprofileprivilege(TRUE);
		} else if ("sesystemtimeprivilege".equalsIgnoreCase(privilege)) {
		    item.setSesystemtimeprivilege(TRUE);
		} else if ("setakeownershipprivilege".equalsIgnoreCase(privilege)) {
		    item.setSetakeownershipprivilege(TRUE);
		} else if ("setcbprivilege".equalsIgnoreCase(privilege)) {
		    item.setSetcbprivilege(TRUE);
		} else if ("setimezoneprivilege".equalsIgnoreCase(privilege)) {
		    item.setSetimezoneprivilege(TRUE);
		} else if ("seundockprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeundockprivilege(TRUE);
		} else if ("seunsolicitedinputprivilege".equalsIgnoreCase(privilege)) {
		    item.setSeunsolicitedinputprivilege(TRUE);
		} else if ("sebatchlogonright".equalsIgnoreCase(privilege)) {
		    item.setSebatchlogonright(TRUE);
		} else if ("seinteractivelogonright".equalsIgnoreCase(privilege)) {
		    item.setSeinteractivelogonright(TRUE);
		} else if ("senetworklogonright".equalsIgnoreCase(privilege)) {
		    item.setSenetworklogonright(TRUE);
		} else if ("seremoteinteractivelogonright".equalsIgnoreCase(privilege)) {
		    item.setSeremoteinteractivelogonright(TRUE);
		} else if ("seservicelogonright".equalsIgnoreCase(privilege)) {
		    item.setSeservicelogonright(TRUE);
		} else if ("sedenybatchLogonright".equalsIgnoreCase(privilege)) {
		    item.setSedenybatchLogonright(TRUE);
		} else if ("sedenyinteractivelogonright".equalsIgnoreCase(privilege)) {
		    item.setSedenyinteractivelogonright(TRUE);
		} else if ("sedenynetworklogonright".equalsIgnoreCase(privilege)) {
		    item.setSedenynetworklogonright(TRUE);
		} else if ("sedenyremoteInteractivelogonright".equalsIgnoreCase(privilege)) {
		    item.setSedenyremoteInteractivelogonright(TRUE);
		} else if ("sedenyservicelogonright".equalsIgnoreCase(privilege)) {
		    item.setSedenyservicelogonright(TRUE);
		} else if ("setrustedcredmanaccessnameright".equalsIgnoreCase(privilege)) {
		    item.setSetrustedcredmanaccessnameright(TRUE);
		} else {
		    session.getLogger().warn(JOVALMsg.ERROR_WIN_ACCESSTOKEN_TOKEN, privilege);
		}
	    }
	}
	return item;
    }

    /**
     * Initialize the adapter and install the probe on the target host.
     */
    private void init() {
	itemCache = new Hashtable<String, AccesstokenItem>();
	errors = new Hashtable<String, MessageType>();

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Get-AccessTokens
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
		runspace.loadModule(getClass().getResourceAsStream("Accesstoken.psm1"));
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Canonicalize the principal name according to the OVAL specification instructions.
     */
    private String getCanonicalizedPrincipalName(IPrincipal p) {
	switch(p.getType()) {
	  case USER:
	    if (directory.isBuiltinUser(p.getNetbiosName())) {
		return p.getName();
	    } else {
		return p.getNetbiosName();
	    }
	  case GROUP:
	  default:
	    if (directory.isBuiltinGroup(p.getNetbiosName())) {
		return p.getName();
	    } else {
		return p.getNetbiosName();
	    }
	}
    }
}
