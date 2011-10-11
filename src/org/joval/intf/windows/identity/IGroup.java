// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

import java.util.Collection;

/**
 * The IGroup interface provides information about a Windows group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IGroup extends IPrincipal {
    /**
     * Non-recursive.
     */
    public Collection<String> getMemberUserNetbiosNames();

    /**
     * Non-recursive.
     */
    public Collection<String> getMemberGroupNetbiosNames();
}
