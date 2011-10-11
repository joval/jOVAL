// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

import java.util.List;

/**
 * Super-interface for users and groups.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPrincipal {
    public enum Type {
	USER, GROUP;
    }

    public String getNetbiosName();

    public String getDomain();

    public String getName();

    public String getSid();

    public Type getType();
}
