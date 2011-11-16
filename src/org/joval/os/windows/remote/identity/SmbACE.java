// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.identity;

import jcifs.smb.ACE;

import org.joval.intf.windows.identity.IACE;

/**
 * JCIFS-based implementation of an IACE.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmbACE implements IACE {
    private ACE ace;

    public SmbACE(ACE ace) {
	this.ace = ace;
    }

    // Implement IACE

    public int getFlags() {
	return ace.getFlags();
    }

    public int getAccessMask() {
	return ace.getAccessMask();
    }

    public String getSid() {
	return ace.getSID().toString();
    }
}
