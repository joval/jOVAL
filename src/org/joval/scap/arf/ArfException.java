// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

import org.joval.scap.ScapException;

/**
 * An exception class for ARF parsing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ArfException extends ScapException {
    public ArfException(String message) {
	super(message);
    }

    public ArfException(Exception e) {
	super(e);
    }
}
