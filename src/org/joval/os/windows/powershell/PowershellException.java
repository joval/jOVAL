// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.powershell;

/**
 * An exception class for Powershell operations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PowershellException extends Exception {
    public PowershellException(String message) {
	super(message);
    }

    public PowershellException(Throwable cause) {
	super(cause);
    }
}
