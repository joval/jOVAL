// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.cpe;

import org.joval.scap.ScapException;

/**
 * An exception class for CPE parsing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CpeException extends ScapException {
    public CpeException(String message) {
	super(message);
    }

    public CpeException(Exception e) {
	super(e);
    }
}
