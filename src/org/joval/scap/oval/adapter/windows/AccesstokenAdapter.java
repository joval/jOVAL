// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.identity.IDirectory;
import jsaf.intf.windows.identity.IGroup;
import jsaf.intf.windows.identity.IPrincipal;
import jsaf.intf.windows.identity.IUser;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.provider.windows.wmi.WmiException;
import jsaf.util.Base64;
import jsaf.util.IniFile;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.AccesstokenBehaviors;
import scap.oval.definitions.windows.AccesstokenObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.AccesstokenItem;

import org.joval.intf.plugin.IAdapter;
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
    private Map<String, AccesstokenItem> itemCache;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(AccesstokenObject.class);
	} else {
	    notapplicable.add(AccesstokenObject.class);
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
	Map<String, AccesstokenItem> items = new HashMap<String, AccesstokenItem>();

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
	    Collection<IPrincipal> principals = new ArrayList<IPrincipal>();
	    switch(op) {
	      case EQUALS:
		principals.add(directory.queryPrincipal(principalStr));
		break;

	      case NOT_EQUAL:
	      case PATTERN_MATCH:
		Collection<IPrincipal> allPrincipals = new ArrayList<IPrincipal>();
		allPrincipals.addAll(directory.queryAllPrincipals());
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

	    //
	    // Determine principals for which items are cached, and which must be queried
	    //
	    Map<String, IPrincipal> queryPrincipals = new HashMap<String, IPrincipal>();
	    for (IPrincipal p : principals) {
		for (IPrincipal principal : directory.getAllPrincipals(p, include, resolve)) {
		    String sid = principal.getSid();
		    if (itemCache.containsKey(sid)) {
			items.put(sid, itemCache.get(sid));
		    } else {
			queryPrincipals.put(sid, principal);
		    }
		}
	    }

	    //
	    // Query tokens for non-cached principals (if there are any)
	    //
	    if (queryPrincipals.size() > 0) {
		StringBuffer cmd = new StringBuffer();
		for (IPrincipal p : queryPrincipals.values()) {
		    if (cmd.length() > 0) {
			cmd.append(",");
		    }
		    cmd.append("\"").append(p.getSid()).append("\"");
		}
		cmd.append(" | Get-AccessTokens | Transfer-Encode");
		IniFile data = new IniFile(new ByteArrayInputStream(Base64.decode(runspace.invoke(cmd.toString()))));
		for (IPrincipal p : queryPrincipals.values()) {
		    String sid = p.getSid();
		    if (data.containsSection(sid)) {
			AccesstokenItem item = makeItem(p, data.getSection(sid));
			itemCache.put(sid, item);
			items.put(sid, item);
		    }
		}
	    }
	} catch (PowershellException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_WIN_ACCESSTOKEN_PRINCIPAL, principalStr, e.getMessage());
	    msg.setValue(s);
	    rc.addMessage(msg);
	    session.getLogger().warn(s);
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
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
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

    private AccesstokenItem makeItem(IPrincipal principal, IProperty tokens) throws Exception {
	AccesstokenItem item = Factories.sc.windows.createAccesstokenItem();
	EntityItemStringType principalType = Factories.sc.core.createEntityItemStringType();
	String principalName = getCanonicalizedPrincipalName(principal);
	session.getLogger().debug(JOVALMsg.STATUS_WIN_ACCESSTOKEN, principalName);
	principalType.setValue(principalName);
	item.setSecurityPrincipal(principalType);

	//
	// Set defaults
	//
	item.setSeassignprimarytokenprivilege(FALSE);
	item.setSeauditprivilege(FALSE);
	item.setSebackupprivilege(FALSE);
	item.setSechangenotifyprivilege(FALSE);
	item.setSecreateglobalprivilege(FALSE);
	item.setSecreatepagefileprivilege(FALSE);
	item.setSecreatepermanentprivilege(FALSE);
	item.setSecreatesymboliclinkprivilege(FALSE);
	item.setSecreatetokenprivilege(FALSE);
	item.setSedebugprivilege(FALSE);
	item.setSeenabledelegationprivilege(FALSE);
	item.setSeimpersonateprivilege(FALSE);
	item.setSeincreasebasepriorityprivilege(FALSE);
	item.setSeincreasequotaprivilege(FALSE);
	item.setSeincreaseworkingsetprivilege(FALSE);
	item.setSeloaddriverprivilege(FALSE);
	item.setSelockmemoryprivilege(FALSE);
	item.setSemachineaccountprivilege(FALSE);
	item.setSemanagevolumeprivilege(FALSE);
	item.setSeprofilesingleprocessprivilege(FALSE);
	item.setSerelabelprivilege(FALSE);
	item.setSeremoteshutdownprivilege(FALSE);
	item.setSerestoreprivilege(FALSE);
	item.setSesecurityprivilege(FALSE);
	item.setSeshutdownprivilege(FALSE);
	item.setSesyncagentprivilege(FALSE);
	item.setSesystemenvironmentprivilege(FALSE);
	item.setSesystemprofileprivilege(FALSE);
	item.setSesystemtimeprivilege(FALSE);
	item.setSetakeownershipprivilege(FALSE);
	item.setSetcbprivilege(FALSE);
	item.setSetimezoneprivilege(FALSE);
	item.setSeundockprivilege(FALSE);
	item.setSeunsolicitedinputprivilege(FALSE);
	item.setSebatchlogonright(FALSE);
	item.setSeinteractivelogonright(FALSE);
	item.setSenetworklogonright(FALSE);
	item.setSeremoteinteractivelogonright(FALSE);
	item.setSeservicelogonright(FALSE);
	item.setSedenybatchLogonright(FALSE);
	item.setSedenyinteractivelogonright(FALSE);
	item.setSedenynetworklogonright(FALSE);
	item.setSedenyremoteInteractivelogonright(FALSE);
	item.setSedenyservicelogonright(FALSE);
	item.setSetrustedcredmanaccessnameright(FALSE);

	//
	// Set discoverd tokens to TRUE
	//
	for (String privilege : tokens) {
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
	    } else if ("sedenyremoteinteractivelogonright".equalsIgnoreCase(privilege)) {
		item.setSedenyremoteInteractivelogonright(TRUE);
	    } else if ("sedenyservicelogonright".equalsIgnoreCase(privilege)) {
		item.setSedenyservicelogonright(TRUE);
	    } else if ("setrustedcredmanaccessprivilege".equalsIgnoreCase(privilege)) {
		item.setSetrustedcredmanaccessnameright(TRUE);
	    } else {
		session.getLogger().warn(JOVALMsg.ERROR_WIN_ACCESSTOKEN_TOKEN, privilege);
	    }
	}
	return item;
    }

    /**
     * Initialize the adapter and install the probe on the target host.
     */
    private void init() {
	itemCache = new HashMap<String, AccesstokenItem>();

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Get-AccessTokens
	// Powershell module code.
	//
	IWindowsSession.View view = session.getNativeView();
	for (IRunspace rs : session.getRunspacePool().enumerate()) {
	    if (rs.getView() == view) {
		runspace = rs;
		break;
	    }
	}
	try {
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn(view);
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
	    if (p.isBuiltin()) {
		return p.getName();
	    } else {
		return p.getNetbiosName();
	    }
	  case GROUP:
	  default:
	    if (p.isBuiltin()) {
		return p.getName();
	    } else {
		return p.getNetbiosName();
	    }
	}
    }
}
