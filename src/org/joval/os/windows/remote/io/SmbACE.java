// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.io;

import jcifs.smb.ACE;

import org.joval.intf.windows.identity.IACE;

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

    public boolean isAllow() {
	return ace.isAllow();
    }

    public boolean isInherited() {
	return ace.isInherited();
    }
}
