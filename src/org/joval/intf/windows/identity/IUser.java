// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

import java.util.Collection;

/**
 * The IUser interface provides information about a Windows user.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUser extends IPrincipal {
    public Collection<String> getGroupNetbiosNames();

    public boolean isEnabled();
}
