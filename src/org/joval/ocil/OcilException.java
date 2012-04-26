// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

/**
 * An exception class for OCIL parsing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OcilException extends Exception {
    public OcilException(String message) {
	super(message);
    }

    public OcilException(Exception e) {
	super(e);
    }
}
