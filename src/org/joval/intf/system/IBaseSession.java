// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

/**
 * A representation of a basic session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBaseSession {
    public boolean connect();

    public void disconnect();

    public IProcess createProcess(String command) throws Exception;

    public Type getType();

    enum Type {
	SSH("ssh"),
	UNIX("unix"),
	CISCO_IOS("ios"),
	WINDOWS("windows"),
	UNKNOWN("unknown");

	private String s;

	Type(String s) {
	    this.s = s;
	}

	public String toString() {
	    return s;
	}

	public static Type getType(String s) {
	    for (Type t : values()) {
		if (t.s.equals(s)) {
		    return t;
		}
	    }
	    return UNKNOWN;
	}
    }
}
