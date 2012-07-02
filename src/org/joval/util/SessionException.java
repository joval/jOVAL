// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

/**
 * An exception indicating there is a problem with an IBaseSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SessionException extends Exception {
    public SessionException(String message) {
	super(message);
    }

    public SessionException(Exception e) {
	super(e);
    }

    @Override
    public String getMessage() {
	String s = super.getMessage();
	if (s == null) {
	    return getMessage(getCause());
	} else {
	    return s;
	}
    }

    // Private

    private String getMessage(Throwable t) {
	if (t == null) {
	    return null;
	} else if (t.getMessage() == null) {
	    String s = getMessage(t.getCause());
	    if (s == null) {
		return t.getClass().getName();
	    } else {
		return s;
	    }
	} else {
	    return t.getMessage();
	}
    }
}
