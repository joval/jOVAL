// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

/**
 * An interface representing of a basic session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBaseSession {
    /**
     * A constant defining the String "localhost".
     */
    String LOCALHOST = "localhost";

    /**
     * Connect the session.
     */
    boolean connect();

    /**
     * Disconnect the session.
     */
    void disconnect();

    /**
     * Get the name of the host to which the session is connected.
     */
    String getHostname();

    /**
     * Create a process on the machine.
     */
    IProcess createProcess(String command) throws Exception;

    /**
     * Get the session type.
     */
    Type getType();

    /**
     * An enumeration of potential session types.
     */
    enum Type {
	/**
	 * An SSH-type session.  This can potentially become a UNIX or CISCO_IOS after discovery (if the IBaseSession also
	 * implements ILocked, discovery occurs when the getType method is invoked after the session is unlocked with an
	 * ICredential).
	 *
	 * @see ICredential
	 */
	SSH("ssh"),

	/**
	 * Indicates a session with a Unix host.
	 */
	UNIX("unix"),

	/**
	 * Indicates a session with a device running Cisco IOS.
	 */
	CISCO_IOS("ios"),

	/**
	 * Indicates a session with a Windows host.
	 */
	WINDOWS("windows"),

	/**
	 * Indicates that the session type cannot be determined.
	 */
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
