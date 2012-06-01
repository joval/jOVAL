// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

/**
 * The parent exception class for all SCAP-related exceptions.  Also used for exceptions originating
 * from a Data Stream.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ScapException extends Exception {
    public ScapException(String message) {
	super(message);
    }

    public ScapException(Exception e) {
	super(e);
    }
}
