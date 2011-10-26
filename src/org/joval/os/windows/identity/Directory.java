// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.joval.intf.windows.identity.IACE;
import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.os.windows.wmi.WmiException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Implementation of IDirectory.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Directory implements IDirectory {
    /**
     * Get the Name portion of a Domain\\Name String.  If there is no domain portion, returns the original String.
     */
    public static String getName(String s) {
	int ptr = s.indexOf("\\");
	if (ptr == -1) {
	    return s;
	} else {
	    return s.substring(ptr+1);
	}
    }

    private IWindowsSession session;
    private IWmiProvider wmi;
    private ActiveDirectory ad;
    private LocalDirectory local;

    public Directory(IWindowsSession session) {
	this.session = session;
    }

    public boolean connect() {
	wmi = session.getWmiProvider();
	if (wmi.connect()) {
	    ad = new ActiveDirectory(wmi);
	    local = new LocalDirectory(session.getSystemInfo().getPrimaryHostName(), wmi);
	    return true;
	} else {
	    return false;
	}
    }

    public void disconnect() {
	if (wmi != null) {
	    wmi.disconnect();
	    wmi = null;
	}
	ad = null;
	local = null;
    }

    // Implement IDirectory

    public IUser queryUserBySid(String sid) throws NoSuchElementException, WmiException {
	try {
	    return local.queryUserBySid(sid);
	} catch (NoSuchElementException e) {
	    return ad.queryUserBySid(sid);
	}
    }

    public IUser queryUser(String netbiosName) throws IllegalArgumentException, NoSuchElementException, WmiException {
	if (isLocal(netbiosName)) {
	    return local.queryUser(netbiosName);
	} else {
	    return ad.queryUser(netbiosName);
	}
    }

    public Collection<IUser> queryAllUsers() throws WmiException {
	return local.queryAllUsers();
    }

    public IGroup queryGroupBySid(String sid) throws NoSuchElementException, WmiException {
	try {
	    return local.queryGroupBySid(sid);
	} catch (NoSuchElementException e) {
	    return ad.queryGroupBySid(sid);
	}
    }

    public IGroup queryGroup(String netbiosName) throws IllegalArgumentException, NoSuchElementException, WmiException {
	if (isLocal(netbiosName)) {
	    return local.queryGroup(netbiosName);
	} else {
	    return ad.queryGroup(netbiosName);
	}
    }

    public Collection<IGroup> queryAllGroups() throws WmiException {
	return local.queryAllGroups();
    }

    public IPrincipal queryPrincipal(String netbiosName) throws NoSuchElementException, WmiException {
	if (isLocal(netbiosName)) {
	    return local.queryPrincipal(netbiosName);
	} else {
	    return ad.queryPrincipal(netbiosName);
	}
    }

    public IPrincipal queryPrincipalBySid(String sid) throws NoSuchElementException, WmiException {
	try {
	    return local.queryPrincipalBySid(sid);
	} catch (NoSuchElementException e) {
	    return ad.queryPrincipalBySid(sid);
	}
    }

    public Collection<IPrincipal> queryAllPrincipals() throws WmiException {
	return local.queryAllPrincipals();
    }

    public boolean isBuiltinUser(String netbiosName) {
	return local.isBuiltinUser(netbiosName);
    }

    public boolean isBuiltinGroup(String netbiosName) {
	return local.isBuiltinGroup(netbiosName);
    }

    public boolean isLocal(String netbiosName) {
	return local.isMember(netbiosName);
    }

    public boolean isLocalSid(String sid) {
	return local.isMemberSid(sid);
    }

    public boolean isDomainMember(String netbiosName) {
	return ad.isMember(netbiosName);
    }

    public boolean isDomainSid(String sid) {
	return ad.isMemberSid(sid);
    }

    public String getQualifiedNetbiosName(String netbiosName) {
	return local.getQualifiedNetbiosName(netbiosName);
    }

    public Collection<IPrincipal> getAllPrincipals(IPrincipal principal, boolean includeGroups, boolean resolveGroups)
		throws WmiException {

	Hashtable<String, IPrincipal> principals = new Hashtable<String, IPrincipal>();
	getAllPrincipalsInternal(principal, resolveGroups, principals);
	Collection<IPrincipal> results = new Vector<IPrincipal>();
	for (IPrincipal p : principals.values()) {
	    switch(p.getType()) {
	      case GROUP:
		if (includeGroups) {
		    results.add(p);
		}
		break;

	      case USER:
		results.add(p);
		break;
	    }
	}
	return results;
    }

    public boolean isApplicable(IPrincipal principal, IACE entry) throws WmiException {
	if (principal.getSid().equals(entry.getSid())) {
	    return true;
	} else {
	    try {
		return getAllPrincipals(queryPrincipalBySid(entry.getSid()), true, true).contains(principal);
	    } catch (NoSuchElementException e) {
	    }
	    return false;
	}
    }

    // Private

    /**
     * Won't get stuck in a loop because it adds the groups themselves to the Hashtable as it goes.
     */
    private void getAllPrincipalsInternal(IPrincipal principal, boolean resolve, Hashtable<String, IPrincipal> principals)
		throws WmiException {

	if (!principals.containsKey(principal.getSid())) {
	    principals.put(principal.getSid(), principal);
	    switch(principal.getType()) {
	      case GROUP:
		if (resolve) {
		    principals.put(principal.getSid(), principal);
		    IGroup g = (IGroup)principal;
		    for (String netbiosName : g.getMemberUserNetbiosNames()) {
			try {
			    getAllPrincipalsInternal(queryUser(netbiosName), resolve, principals);
			} catch (IllegalArgumentException e) {
			    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			} catch (NoSuchElementException e) {
			    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			}
		    }
		    for (String netbiosName : g.getMemberGroupNetbiosNames()) {
			try {
			    getAllPrincipalsInternal(queryGroup(netbiosName), resolve, principals);
			} catch (IllegalArgumentException e) {
			    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			} catch (NoSuchElementException e) {
			    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			}
		    }
		}
		break;
	    }
	}
    }
}
