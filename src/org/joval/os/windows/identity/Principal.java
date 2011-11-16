// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.identity;

import java.util.List;

import org.joval.intf.windows.identity.IPrincipal;
import org.joval.io.LittleEndian;

/**
 * The abstract parent class for User and Group.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class Principal implements IPrincipal {
    /**
     * Convert a hexidecimal String representation of a SID into a "readable" SID String.
     *
     * The WMI implementations return this kind of String when a binary is fetched using getAsString.
     * @see org.joval.intf.windows.wmi.ISWbemProperty
     */
    public static String toSid(String hex) {
	int len = hex.length();
	if (len % 2 == 1) {
	    throw new IllegalArgumentException(hex);
	}

	byte[] raw = new byte[len/2];
	int index = 0;
	for (int i=0; i < len; i+=2) {
	    String s = hex.substring(i, i+2);
	    raw[index++] = (byte)(Integer.parseInt(s, 16) & 0xFF);
	}

	return toSid(raw);
    }

    /**
     * Convert a byte[] representation of a SID into a "readable" SID String.
     */
    public static String toSid(byte[] raw) {
	int rev = raw[0];
	int subauthCount = raw[1];

	StringBuffer sb = new StringBuffer();
	for (int i=2; i < 8; i++) {
	    sb.append(LittleEndian.toHexString(raw[i]));
	}
	String idAuthStr = sb.toString();
	long idAuth = Long.parseLong(idAuthStr, 16);

	StringBuffer sid = new StringBuffer("S-");
	sid.append(Integer.toHexString(rev));
	sid.append("-");
	sid.append(Long.toHexString(idAuth));

	for (int i=0; i < subauthCount; i++) {
	    sid.append("-");
	    byte[] buff = new byte[4];
	    int base = 8 + i*4;
	    buff[0] = raw[base];
	    buff[1] = raw[base + 1];
	    buff[2] = raw[base + 2];
	    buff[3] = raw[base + 3];
	    sid.append(Long.toString(LittleEndian.getUInt(buff) & 0xFFFFFFFFL));
	}
	return sid.toString();
    }

    String domain, name, sid;

    Principal(String domain, String name, String sid) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
    }

    public boolean equals(Object other) {
	if (other instanceof IPrincipal) {
	    return sid.equals(((IPrincipal)other).getSid());
	} else {
	    return super.equals(other);
	}
    }

    // Implement IPrincipal

    public String getNetbiosName() {
	return domain + "\\" + name;
    }

    public String getDomain() {
	return domain;
    }

    public String getName() {
	return name;
    }

    public String getSid() {
	return sid;
    }

    public abstract Type getType();
}
