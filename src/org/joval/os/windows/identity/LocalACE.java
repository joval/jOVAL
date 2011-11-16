// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import com.sun.jna.platform.win32.WinNT;

import org.joval.intf.windows.identity.IACE;

public class LocalACE implements IACE {
    private WinNT.ACCESS_ACEStructure ace;

    public LocalACE(WinNT.ACCESS_ACEStructure ace) {
	this.ace = ace;
    }

    // Implement IACE

    public int getFlags() {
	return (int)(ace.AceFlags & 0xFF);
    }

    public int getAccessMask() {
	return ace.Mask;
    }

    public String getSid() {
	return Principal.toSid(ace.getSID().getBytes());
    }
}
