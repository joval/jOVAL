// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

import java.util.Collection;
import java.util.NoSuchElementException;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.windows.identity.IDirectory;
import org.joval.intf.windows.identity.IGroup;
import org.joval.intf.windows.identity.IPrincipal;
import org.joval.intf.windows.identity.IUser;
import org.joval.os.windows.wmi.WmiException;

/**
 * Representation of a Windows user/group store.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDirectory {
    /**
     * Returns the user corresponding to the specified SID.
     */
    public IUser queryUserBySid(String sid) throws NoSuchElementException, WmiException;

    /**
     * Query for an individual user.  The input parameter should be of the form DOMAIN\\name.  For built-in users, the
     * DOMAIN\\ part can be dropped, in which case the name parameter is just the user name.
     *
     * @throws IllegalArgumentException if the domain is not recognized
     * @throws NoSuchElementException if the group does not exist
     */
    public IUser queryUser(String netbiosName) throws IllegalArgumentException, NoSuchElementException, WmiException;

    /**
     * Returns a Collection of all the local users.
     */
    public Collection<IUser> queryAllUsers() throws WmiException;

    /**
     * Returns the group corresponding to the specified SID.
     */
    public IGroup queryGroupBySid(String sid) throws NoSuchElementException, WmiException;

    /**
     * Query for an individual group.  The input parameter should be of the form DOMAIN\\name.  For built-in groups, the
     * DOMAIN\\ part can be dropped, in which case the name parameter is just the group name.
     *
     * @throws IllegalArgumentException if the domain is not recognized
     * @throws NoSuchElementException if the group does not exist
     */
    public IGroup queryGroup(String netbiosName) throws IllegalArgumentException, NoSuchElementException, WmiException;

    /**
     * Returns a Collection of all the local groups.
     */
    public Collection<IGroup> queryAllGroups() throws WmiException;

    /**
     * Returns a Principal (User or Group) given a Netbios name.
     */
    public IPrincipal queryPrincipal(String netbiosName) throws NoSuchElementException, WmiException;

    /**
     * Returns a Principal (User or Group) given a sid.
     */
    public IPrincipal queryPrincipalBySid(String sid) throws NoSuchElementException, WmiException;

    /**
     * Returns a Collection of all local users and groups.
     */
    public Collection<IPrincipal> queryAllPrincipals() throws WmiException;

    /**
     * Returns true of the supplied NetBios username String represents a built-in account on the local machine.
     */
    public boolean isBuiltinUser(String netbiosName);

    /**
     * Returns true if the supplied NetBios group name String represents a built-in group on the local machine.
     */
    public boolean isBuiltinGroup(String netbiosName);

    /**
     * Does the local machine recognize this principal?
     */
    public boolean isLocal(String netbiosName);

    /**
     * Does the local machine recognize this SID?
     */
    public boolean isLocalSid(String sid);

    /**
     * Does the machine's domain server recognize this principal?
     */
    public boolean isDomainMember(String netbiosName);

    /**
     * Does the machine's domain server recognize this SID?
     */
    public boolean isDomainSid(String sid);

    /**
     * Fills in the domain with the local hostname if it is not specified in the argument.
     */
    public String getQualifiedNetbiosName(String netbiosName);
}
