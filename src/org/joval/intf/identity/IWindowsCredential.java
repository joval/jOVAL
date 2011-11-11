// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.identity;

/**
 * A representation of a Windows credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IWindowsCredential extends ICredential {
    String getDomain();

    /**
     * Return a username of the form domain\name.
     */
    String getDomainUser();
}
