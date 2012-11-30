// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import org.joval.intf.windows.identity.IACE;

/**
 * A Windows Access Control Entry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ACE implements IACE {
    private String sid;
    private int accessMask;

    public ACE(String sid, int accessMask) {
	this.sid = sid;
	this.accessMask = accessMask;
    }

    /**
     * Create an ACE from a string of the form:
     * "ACE: mask=131097,sid=S-1-1-0"
     */
    public ACE(String s) throws IllegalArgumentException {
	int begin = s.indexOf("mask=") + 5;
	int end = s.indexOf(",sid=");
	if (begin < 0 || end < 0) {
	    throw new IllegalArgumentException(s);
	} else {
	    accessMask = Integer.parseInt(s.substring(begin, end));
	    begin = end + 5;
	    sid = s.substring(begin);
	}
    }

    // Implement IACE

    public int getAccessMask() {
	return accessMask;
    }

    public String getSid() {
	return sid;
    }
}
