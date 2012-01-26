// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import oval.schemas.common.FamilyEnumeration;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.util.ILoggable;
import org.joval.intf.util.IProperty;

/**
 * An interface representing of a basic session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBaseSession extends ILoggable {
    /**
     * An enumeration of timeouts.
     */
    public enum Timeout {
	S, M, L, XL;
    }

    /**
     * Get the timeout value corresponding to the Timeout enumeration.
     */
    long getTimeout(Timeout to);

    /**
     * Property key used to define a "small" amount of time.
     */
    String PROP_READ_TIMEOUT_S = "read.timeout.small";

    /**
     * Property key used to define a "medium" amount of time.
     */
    String PROP_READ_TIMEOUT_M = "read.timeout.medium";

    /**
     * Property key used to define a "large" amount of time.
     */
    String PROP_READ_TIMEOUT_L = "read.timeout.large";

    /**
     * Property key used to define an "extra-large" amount of time.
     */
    String PROP_READ_TIMEOUT_XL = "read.timeout.xl";

    /**
     * Property indicating whether the session should run in debug mode (true/false).
     */
    String PROP_DEBUG = "debug";

    /**
     * Property indicating the number of times to re-try running a command in the event of an unexpected disconnect.
     */
    String PROP_EXEC_RETRIES = "exec.retries";

    /**
     * Property indicating the number of milliseconds to wait for a command to begin to return data.
     */
    public static final String PROP_READ_TIMEOUT = "read.timeout";

    /**
     * A constant defining the String "localhost".
     */
    String LOCALHOST = "localhost";

    /**
     * Check if the session is using debugging mode.
     */
    boolean isDebug();

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

    IProperty getProperties();

    /**
     * Create a process on the machine.
     */
    IProcess createProcess(String command) throws Exception;

    /**
     * Fetch OVAL SystemInformation for the session.
     */
    public SystemInfoType getSystemInfo();

    /**
     * Return the FamilyEnumeration member against which the host should be tested for FamilyTest applicability.
     */
    public FamilyEnumeration getFamily();

    /**
     * Get the session type.
     */
    Type getType();

    /**
     * An enumeration of possible session types.
     */
    enum Type {
	/**
	 * An SSH-type session.  This can potentially become a UNIX or CISCO_IOS after discovery (if the IBaseSession also
	 * implements ILocked, discovery occurs when the getType method is invoked after the session is unlocked with an
	 * ICredential).
	 *
	 * @see org.joval.intf.identity.ICredential
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
