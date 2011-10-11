// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.identity.IUser;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.os.windows.wmi.WmiException;

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
    private ActiveDirectory ad;
    private LocalDirectory local;

    public Directory(IWindowsSession session) {
	this.session = session;
    }

    public void connect() {
	ad = new ActiveDirectory(session.getWmiProvider());
	local = new LocalDirectory(session.getSystemInfo().getPrimaryHostName(), session.getWmiProvider());
    }

    public void disconnect() {
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
}
