// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.sce;

import org.joval.scap.ScapException;

/**
 * An exception class for SCE parsing.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SceException extends ScapException {
    public SceException(String message) {
	super(message);
    }

    public SceException(Exception e) {
	super(e);
    }
}
