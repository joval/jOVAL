// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.intf.system;

import java.io.File;

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
     * A constant defining the String "localhost".
     */
    String LOCALHOST = "localhost";

    /**
     * Check if the session is using debugging mode.
     */
    boolean isDebug();

    /**
     * Returns whether or not the session is connected.
     */
    boolean isConnected();

    /**
     * Returns the account name used by this session, if any.
     */
    String getUsername();

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
     * Access the instance properties controlling various session behaviors (defined by the PROP_* constants).
     */
    IProperty getProperties();

    /**
     * Get the number of milliseconds since midnight 1/1/70 UTC, on the system.
     */
    long getTime() throws Exception;

    /**
     * Does the session terminal echo the command?
     */
    boolean echo();

    /**
     * Create a process on the machine, with the specified environment variables.
     *
     * @param env Environment variables, each of the form "VARIABLE=VALUE".
     */
    IProcess createProcess(String command, String[] env) throws Exception;

    /**
     * Get a directory in which state information can be safely stored.  Returns null if this is a stateless session.
     */
    File getWorkspace();

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
	 * Indicates a session with a device running Cisco IOS.
	 */
	JUNIPER_JUNOS("junos"),

	/**
	 * Indicates a session with a device running Apple iOS.
	 */
	APPLE_IOS("apple_iOS"),

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

	public String value() {
	    return s;
	}

	public static Type typeOf(String s) {
	    for (Type t : values()) {
		if (t.s.equals(s)) {
		    return t;
		}
	    }
	    return UNKNOWN;
	}
    }

    /**
     * When you're completely finished using the session, call this method to clean up caches and other resources.
     */
    void dispose();
}
