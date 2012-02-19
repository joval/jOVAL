// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.cpe;

/**
 * An exception class for CPE parsing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CpeException extends Exception {
    public CpeException(String message) {
	super(message);
    }

    public CpeException(Exception e) {
	super(e);
    }
}
