// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.wmi;

/**
 * An exception class for an IWmiProvider.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class WmiException extends Exception {
    public WmiException(String message) {
	super(message);
    }

    public WmiException(Exception e) {
	super(e);
    }
}
