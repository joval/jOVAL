// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

/**
 * An exception class for variable resolution, used to propagate messages upstream to the object.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ResolveException extends Exception {
    public ResolveException(String message) {
	super(message);
    }

    public ResolveException(Exception e) {
	super(e);
    }
}
