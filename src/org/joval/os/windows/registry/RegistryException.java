// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.registry;

/**
 * Exception class for the Windows registry.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegistryException extends Exception {
    public RegistryException() {
	super();
    }

    public RegistryException(String message) {
	super(message);
    }

    public RegistryException(Exception cause) {
	super(cause);
    }
}
