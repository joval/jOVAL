// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.identity;

import java.security.AccessControlException;

import org.joval.intf.system.IBaseSession;

/**
 * An interface for a credential storage mechanism.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ICredentialStore {
    /**
     * Return the appropriate credential for the specified IBaseSession.
     *
     * @returns null if no credential is found
     *
     * @throws AccessControlException if access to the requested credential is not allowed.
     */
    ICredential getCredential(IBaseSession session) throws AccessControlException;
}
